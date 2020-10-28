package it.airgap.beaconsdk.client

import it.airgap.beaconsdk.data.p2p.P2pPeerInfo
import it.airgap.beaconsdk.exception.BeaconException
import it.airgap.beaconsdk.internal.BeaconApp
import it.airgap.beaconsdk.internal.BeaconConfig
import it.airgap.beaconsdk.internal.client.ConnectionClient
import it.airgap.beaconsdk.internal.controller.MessageController
import it.airgap.beaconsdk.internal.storage.ExtendedStorage
import it.airgap.beaconsdk.internal.storage.SharedPreferencesStorage
import it.airgap.beaconsdk.internal.transport.Transport
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.tryResult
import it.airgap.beaconsdk.message.BeaconMessage
import it.airgap.beaconsdk.storage.BeaconStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class BeaconClient internal constructor(
    val name: String,
    val beaconId: String,
    private val connectionClient: ConnectionClient,
    private val messageController: MessageController,
    private val storage: ExtendedStorage,
) {

    fun connect(): Flow<Result<BeaconMessage.Request>> {
        return subscribe().map {
            when (it) {
                is InternalResult.Success -> Result.success(it.value)
                is InternalResult.Error -> Result.failure(BeaconException.Internal(cause = it.error))
            }
        }
    }

    @Throws(BeaconException::class)
    suspend fun respond(message: BeaconMessage.Response) {
        message.senderId = beaconId

        messageController.onResponse(message).mapError { BeaconException.Internal(cause = it) }.getOrThrow()
        connectionClient.send(message)
    }

    suspend fun addPeers(vararg peers: P2pPeerInfo) {
        storage.addP2pPeers(peers.toList())
    }

    suspend fun getPeers(): List<P2pPeerInfo> =
        storage.getP2pPeers()

    suspend fun removePeers(vararg peers: P2pPeerInfo) {
        with (storage) {
            val toRemove = peers.toList().ifEmpty { getP2pPeers() }

            removeP2pPeers { peer -> toRemove.any { it.publicKey == peer.publicKey } }
            removePermissions { permission -> toRemove.any { it.publicKey == permission.appMetadata.senderId } }
        }
    }

    private fun subscribe(): Flow<InternalResult<BeaconMessage.Request>> =
        connectionClient.subscribe()
            .onEach { result ->
                result.flatMapSuspend { messageController.onRequest(it) }.getOrLogError(TAG)
            }
            .map { BeaconMessage.fromInternalResult(it) }

    private suspend fun BeaconMessage.Companion.fromInternalResult(
        beaconRequest: InternalResult<BeaconMessage.Request>
    ): InternalResult<BeaconMessage.Request> =
        beaconRequest.flatMapSuspend {  request ->
            tryResult {
                request.apply {
                    val appMetadata = storage.findAppMetadata { it.senderId == request.senderId }
                    extendWithMetadata(appMetadata)
                }
            }
        }

    companion object {
        internal const val TAG = "BeaconWalletClient"
    }

    class Builder(private val name: String) {
        private var _storage: BeaconStorage? = null
        private var _matrixNodes: List<String> = emptyList()

        fun storage(storage: BeaconStorage): Builder = apply { _storage = storage }

        fun matrixNodes(nodes: List<String>): Builder = apply { _matrixNodes = nodes }
        fun matrixNodes(vararg nodes: String): Builder = apply { _matrixNodes = nodes.toList() }

        suspend fun build(): BeaconClient {
            val beaconApp = BeaconApp.instance
            val storage = _storage ?: SharedPreferencesStorage.create(beaconApp.applicationContext)
            val matrixNodes = _matrixNodes.ifEmpty { BeaconConfig.defaultRelayServers }

            beaconApp.init(name, matrixNodes, storage)

            with (beaconApp.dependencyRegistry) {
                return BeaconClient(
                    name,
                    beaconApp.beaconId,
                    connectionController(Transport.Type.P2P),
                    messageController,
                    extendedStorage,
                )
            }
        }
    }
}

suspend fun BeaconClient(name: String, builderAction: BeaconClient.Builder.() -> Unit = {}): BeaconClient =
    BeaconClient.Builder(name).apply(builderAction).build()

