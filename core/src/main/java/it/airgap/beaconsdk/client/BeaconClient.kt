package it.airgap.beaconsdk.client

import it.airgap.beaconsdk.message.BeaconMessage
import kotlinx.coroutines.flow.Flow

interface BeaconClient {
    val isInitialized: Boolean

    val name: String
    val beaconId: String

    suspend fun init()
    fun init(callback: InitCallback)

    suspend fun connect(): Flow<Result<BeaconMessage.Request>>
    fun connect(listener: OnNewMessageListener)

    suspend fun respond(message: BeaconMessage.Response)
    fun respond(message: BeaconMessage.Response, callback: ResponseCallback)

    interface InitCallback {
        fun onSuccess()
        fun onError(error: Throwable)
    }

    interface OnNewMessageListener {
        fun onNewMessage(message: BeaconMessage.Request)
        fun onError(error: Throwable)
    }

    interface ResponseCallback {
        fun onSuccess()
        fun onError(error: Throwable)
    }
}