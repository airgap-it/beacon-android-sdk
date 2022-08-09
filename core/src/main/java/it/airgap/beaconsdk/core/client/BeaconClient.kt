package it.airgap.beaconsdk.core.client

import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.data.Peer
import it.airgap.beaconsdk.core.exception.BeaconException
import it.airgap.beaconsdk.core.exception.BlockchainNotFoundException
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.controller.ConnectionController
import it.airgap.beaconsdk.core.internal.controller.MessageController
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.message.BeaconIncomingConnectionMessage
import it.airgap.beaconsdk.core.internal.message.BeaconOutgoingConnectionMessage
import it.airgap.beaconsdk.core.internal.serializer.Serializer
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.*
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.DisconnectBeaconMessage
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.transport.data.PairingMessage
import kotlinx.coroutines.flow.*

/**
 * An abstract base for different client types provided in Beacon.
 */
public abstract class BeaconClient<BM : BeaconMessage>(
    public val app: BeaconApplication,
    public val beaconId: String,
    public val beaconScope: BeaconScope,
    protected val connectionController: ConnectionController,
    protected val messageController: MessageController,
    protected val storageManager: StorageManager,
    protected val crypto: Crypto,
    protected val serializer: Serializer,
    protected val configuration: BeaconConfiguration,
) {

    public val name: String
        get() = app.name

    /**
     * Connects with known peers and subscribes to incoming messages
     * returning a [Flow] of received [BM] instances or occurred errors
     * represented as a [Result].
     */
    public fun connect(): Flow<Result<BM>> =
        connectionController.subscribe()
            .map { result -> result.flatMap { messageController.onIncomingMessage(it.origin, ownOrigin(it.origin), it.content) } }
            .onEach { result -> result.getOrNull()?.let { processMessage(it.first, it.second) } }
            .filterWithConfiguration()
            .mapNotNull { result ->
                result.fold(
                    onSuccess = { transformMessage(it.second)?.let(Result.Companion::success) },
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

    public fun serializePairingData(pairingMessage: PairingMessage): String =
        serializer.serialize(pairingMessage).getOrThrow()

    @JvmName("deserializePairingDataInlined")
    public inline fun <reified T : PairingMessage> deserializePairingData(serialized: String): T =
        `serializer$inline`.deserialize<T>(serialized).getOrThrow()

    public fun deserializePairingData(serialized: String): PairingMessage =
        serializer.deserialize<PairingMessage>(serialized).getOrThrow()

    protected open suspend fun processMessage(origin: Origin, message: BeaconMessage): Result<Unit> =
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
            val peerOrigin = Origin.forPeer(peer)

            val message = DisconnectBeaconMessage(
                id = crypto.guid().getOrThrow(),
                senderId = beaconId,
                version = peer.version,
                origin = ownOrigin(peerOrigin),
                destination = peerOrigin,
            )
            send(message, isTerminal = true)
        }

    protected suspend fun send(message: BeaconMessage, isTerminal: Boolean): Result<Unit> =
        messageController.onOutgoingMessage(beaconId, message, isTerminal)
            .flatMap { connectionController.send(BeaconOutgoingConnectionMessage(it)) }

    protected fun <T> Flow<Result<T>>.filterWithConfiguration(): Flow<Result<T>> =
        filterNot { it.shouldBeIgnored() }

    protected fun <T> Result<T>.takeIfNotIgnored(): Result<T>? = takeIf { !shouldBeIgnored() }

    private fun <T> Result<T>.shouldBeIgnored(): Boolean = with(configuration) {
        when {
            exceptionOrNull() is BlockchainNotFoundException && ignoreUnsupportedBlockchains -> true
            else -> false
        }
    }

    private fun ownOrigin(origin: Origin): Origin =
        when (origin) {
            is Origin.Website -> origin.copy(id = app.keyPair.publicKey.toHexString().asString())
            is Origin.Extension -> origin.copy(id = app.keyPair.publicKey.toHexString().asString())
            is Origin.P2P -> origin.copy(id = app.keyPair.publicKey.toHexString().asString())
        }

    public companion object {}

    // -- PublishedApi --

    @PublishedApi
    internal val `serializer$inline`: Serializer
        get() = serializer
}

