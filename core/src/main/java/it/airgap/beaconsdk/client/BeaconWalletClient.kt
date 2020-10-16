package it.airgap.beaconsdk.client

import it.airgap.beaconsdk.compat.storage.ExtendedStorage
import it.airgap.beaconsdk.compat.storage.SharedPreferencesStorage
import it.airgap.beaconsdk.exception.BeaconException
import it.airgap.beaconsdk.internal.BeaconApp
import it.airgap.beaconsdk.internal.BeaconConfig
import it.airgap.beaconsdk.internal.client.ConnectionClient
import it.airgap.beaconsdk.internal.client.SdkClient
import it.airgap.beaconsdk.internal.controller.MessageController
import it.airgap.beaconsdk.internal.di.DependencyRegistry
import it.airgap.beaconsdk.internal.message.beaconmessage.ApiBeaconMessage
import it.airgap.beaconsdk.internal.transport.Transport
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.failWithUninitialized
import it.airgap.beaconsdk.internal.utils.suspendTryInternal
import it.airgap.beaconsdk.message.BeaconMessage
import it.airgap.beaconsdk.storage.BeaconStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class BeaconWalletClient internal constructor(
    override val name: String,
    private val sdkClient: SdkClient,
    private val connectionClient: ConnectionClient,
    private val messageController: MessageController,
    private val storage: ExtendedStorage,
) : BeaconClient {
    private var _isInitialized: Boolean = false
    override val isInitialized: Boolean
        get() = _isInitialized

    override val beaconId: String
        get() = sdkClient.beaconId ?: failWithUninitialized(TAG)

    override suspend fun init() {
        sdkClient.init()
        _isInitialized = true
    }

    override suspend fun connect(): Flow<Result<BeaconMessage.Request>> {
        failIfUninitialized()

        return subscribe().map {
            when (it) {
                is InternalResult.Success -> Result.success(it.value)
                is InternalResult.Error -> Result.failure(BeaconException.Internal(cause = it.error))
            }
        }
    }

    @Throws(BeaconException::class)
    override suspend fun respond(message: BeaconMessage.Response) {
        failIfUninitialized()

        val internalMessage = ApiBeaconMessage.fromBeaconResponse(message, beaconId)
        messageController.onResponse(internalMessage).mapError { BeaconException.Internal(cause = it) }.getOrThrow()
        connectionClient.send(internalMessage)
    }

    private fun subscribe(): Flow<InternalResult<BeaconMessage.Request>> =
        connectionClient.subscribe()
            .onEach { result ->
                result.flatMapSuspend { messageController.onRequest(it) }.getOrLogError(TAG)
            }
            .map { BeaconMessage.fromInternalResult(it) }

    private suspend fun BeaconMessage.Companion.fromInternalResult(
        beaconRequest: InternalResult<ApiBeaconMessage.Request>
    ): InternalResult<BeaconMessage.Request> =
        beaconRequest.flatMapSuspend {  request ->
            suspendTryInternal {
                val appMetadata = storage.findAppMetadata { it.senderId == request.senderId } ?: failWithNoAppMetadata()

                fromInternalBeaconRequest(request, appMetadata)
            }
        }

    private fun failIfUninitialized() {
        if (!isInitialized) failWithUninitialized(TAG)
    }

    private fun failWithNoAppMetadata(): Nothing = throw IllegalArgumentException("No matching app metadata found")

    companion object {
        internal const val TAG = "BeaconWalletClient"
    }

    class Builder(private val name: String) {
        private var _storage: BeaconStorage? = null
        private var _matrixNodes: List<String> = emptyList()

        fun storage(storage: BeaconStorage): Builder = apply { _storage = storage }

        fun matrixNodes(nodes: List<String>): Builder = apply { _matrixNodes = nodes }
        fun matrixNodes(vararg nodes: String): Builder = apply { _matrixNodes = nodes.toList() }

        fun build(): BeaconWalletClient {
            val beaconApp = BeaconApp.instance
            val storage = _storage ?: SharedPreferencesStorage.create(beaconApp.applicationContext)
            val matrixNodes = _matrixNodes.ifEmpty { BeaconConfig.defaultRelayServers }
            val dependencyRegistry = DependencyRegistry(name, matrixNodes, storage)

            return BeaconWalletClient(
                name,
                dependencyRegistry.sdkClient,
                dependencyRegistry.connectionController(Transport.Type.P2P),
                dependencyRegistry.messageController,
                dependencyRegistry.extendedStorage,
            )
        }
    }
}

fun BeaconWalletClient(name: String, builderAction: BeaconWalletClient.Builder.() -> Unit = {}): BeaconWalletClient =
    BeaconWalletClient.Builder(name).apply(builderAction).build()

