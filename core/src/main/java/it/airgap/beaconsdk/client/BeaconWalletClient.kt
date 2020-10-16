package it.airgap.beaconsdk.client

import it.airgap.beaconsdk.exception.BeaconException
import it.airgap.beaconsdk.internal.BeaconApp
import it.airgap.beaconsdk.internal.client.ConnectionClient
import it.airgap.beaconsdk.internal.client.SdkClient
import it.airgap.beaconsdk.internal.controller.MessageController
import it.airgap.beaconsdk.internal.message.beaconmessage.ApiBeaconMessage
import it.airgap.beaconsdk.internal.servicelocator.InternalServiceLocator
import it.airgap.beaconsdk.internal.storage.ExtendedStorage
import it.airgap.beaconsdk.internal.storage.SharedPreferencesStorage
import it.airgap.beaconsdk.internal.transport.Transport
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.failWithUninitialized
import it.airgap.beaconsdk.internal.utils.suspendTryInternal
import it.airgap.beaconsdk.message.BeaconMessage
import it.airgap.beaconsdk.storage.BeaconStorage
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

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

    private val jobs: MutableMap<JobType, CompletableJob> = mutableMapOf()

    override suspend fun init() {
        sdkClient.init()
        _isInitialized = true
    }

    override fun init(callback: BeaconClient.InitCallback) {
        initScope {
            init()
            callback.onSuccess()
        }
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

    override fun connect(listener: BeaconClient.OnNewMessageListener) {
        failIfUninitialized()

        receiveScope {
            subscribe().collect {
                when (it) {
                    is InternalResult.Success -> listener.onNewMessage(it.value)
                    is InternalResult.Error -> listener.onError(BeaconException.fromInternalError(it))
                }
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

    override fun respond(message: BeaconMessage.Response, callback: BeaconClient.ResponseCallback) {
        failIfUninitialized()

        sendScope {
            try {
                respond(message)
                callback.onSuccess()
            } catch (e: Exception) {
                callback.onError(e)
            }
        }
    }

    private fun subscribe(): Flow<InternalResult<BeaconMessage.Request>> =
        connectionClient.subscribe()
            .onEach { result ->
                result.flatMapSuspend { messageController.onRequest(it) }.getOrLogError(TAG)
            }
            .map { BeaconMessage.fromInternalResult(it) }

    private fun initScope(block: suspend () -> Unit) {
        jobScope(JobType.Init, block)
    }

    private fun receiveScope(block: suspend () -> Unit) {
        jobScope(JobType.Receive, block)
    }

    private fun sendScope(block: suspend () -> Unit) {
        jobScope(JobType.Send, block)
    }

    private fun jobScope(type: JobType, block: suspend () -> Unit) {
        val job = jobs.getOrPut(type) { Job() }
        CoroutineScope(job).launch {
            block()
            job.complete()
            jobs.remove(type)
        }
    }

    private suspend fun BeaconMessage.Companion.fromInternalResult(
        beaconRequest: InternalResult<ApiBeaconMessage.Request>
    ): InternalResult<BeaconMessage.Request> =
        beaconRequest.flatMapSuspend {  request ->
            suspendTryInternal {
                val appMetadata = storage.findAppMetadata { it.senderId == request.senderId } ?: failWithNoAppMetadata()

                fromInternalBeaconRequest(request, appMetadata)
            }
        }

    private fun BeaconException.Companion.fromInternalError(internalError: InternalResult.Error<BeaconMessage.Request>): BeaconException =
        BeaconException.Internal(cause = internalError.error)

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
            val serviceLocator = InternalServiceLocator(name, storage, _matrixNodes)

            return BeaconWalletClient(
                name,
                serviceLocator.sdkClient,
                serviceLocator.connectionController(Transport.Type.P2P),
                serviceLocator.messageController,
                serviceLocator.extendedStorage,
            )
        }
    }

    private enum class JobType {
        Init,
        Receive,
        Send,
    }
}

