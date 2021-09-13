package it.airgap.beaconsdk.core.client

import it.airgap.beaconsdk.core.data.beacon.Origin
import it.airgap.beaconsdk.core.data.beacon.Peer
import it.airgap.beaconsdk.core.exception.BeaconException
import it.airgap.beaconsdk.core.internal.controller.ConnectionController
import it.airgap.beaconsdk.core.internal.controller.MessageController
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.message.BeaconConnectionMessage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.flatMap
import it.airgap.beaconsdk.core.internal.utils.launchForEach
import it.airgap.beaconsdk.core.internal.utils.runCatchingFlat
import it.airgap.beaconsdk.core.internal.utils.success
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.DisconnectBeaconMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach

/**
 * An abstract base for different client types provided in Beacon.
 */
public abstract class BeaconClient<BM : BeaconMessage>(
    public val name: String,
    public val beaconId: String,
    protected val connectionController: ConnectionController,
    protected val messageController: MessageController,
    protected val storageManager: StorageManager,
    protected val crypto: Crypto,
) {

    /**
     * Connects with known peers and subscribes to incoming messages
     * returning a [Flow] of received [BM] instances or occurred errors
     * represented as a [Result].
     */
    public fun connect(): Flow<Result<BM>> =
        connectionController.subscribe()
            .map { result -> result.flatMap { messageController.onIncomingMessage(it.origin, it.content) } }
            .onEach { result -> result.getOrNull()?.let { processMessage(it) } }
            .mapNotNull { result ->
                result.fold(
                    onSuccess = { transformMessage(it)?.let(Result.Companion::success) },
                    onFailure = { Result.failure(BeaconException.from(it)) },
                )
            }

    /**
     * Adds new [peers].
     *
     * The new peers will be persisted and subscribed.
     */
    public suspend fun addPeers(vararg peers: Peer) {
        addPeers(peers.toList())
    }

    /**
     * Adds new [peers].
     *
     * The new peers will be persisted and subscribed.
     */
    public suspend fun addPeers(peers: List<Peer>) {
        storageManager.addPeers(peers)
    }

    /**
     * Returns a list of known peers.
     */
    public suspend fun getPeers(): List<Peer> =
        storageManager.getPeers()

    /**
     * Removes the specified [peers].
     *
     * The removed peers will be unsubscribed.
     */
    public suspend fun removePeers(vararg peers: Peer) {
        removePeers(peers.toList())
    }

    /**
     * Removes the specified [peers].
     *
     * The removed peers will be unsubscribed.
     */
    public suspend fun removePeers(peers: List<Peer>) {
        storageManager.removePeers(peers)
        peers.launchForEach { disconnect(it) }
    }

    /**
     * Removes all known peers.
     *
     * All peers will be unsubscribed.
     */
    public suspend fun removeAllPeers() {
        val peers = storageManager.getPeers()
        storageManager.removePeers()
        peers.launchForEach { disconnect(it) }
    }

    protected open suspend fun processMessage(message: BeaconMessage): Result<Unit> =
        when (message) {
            is DisconnectBeaconMessage -> {
                removePeer(message.origin.id)
                Result.success()
            }
            else -> {
                /* no action */
                Result.success()
            }
        }

    protected abstract suspend fun transformMessage(message: BeaconMessage): BM?

    private suspend fun removePeer(publicKey: String) {
        storageManager.removePeers { it.publicKey == publicKey }
    }

    private suspend fun disconnect(peer: Peer): Result<Unit> =
        runCatchingFlat {
            val message = DisconnectBeaconMessage(crypto.guid().getOrThrow(), beaconId, peer.version, Origin.forPeer(peer))
            send(message, isTerminal = true)
        }

    protected suspend fun send(message: BeaconMessage, isTerminal: Boolean): Result<Unit> =
        messageController.onOutgoingMessage(beaconId, message, isTerminal)
            .flatMap { connectionController.send(BeaconConnectionMessage(it)) }

    public companion object {}
}

