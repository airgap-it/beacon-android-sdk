package it.airgap.beaconsdk.compat.client

import it.airgap.beaconsdk.client.BeaconClient
import it.airgap.beaconsdk.compat.storage.BeaconCompatStorage
import it.airgap.beaconsdk.compat.internal.CompatStorageDecorator
import it.airgap.beaconsdk.exception.BeaconException
import it.airgap.beaconsdk.message.BeaconMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

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

fun BeaconClient.init(callback: InitCallback) {
    initScope {
        init()
        callback.onSuccess()
    }
}

fun BeaconClient.connect(listener: OnNewMessageListener) {
    receiveScope {
        try {
            connect().collect {
                when {
                    it.isSuccess -> listener.onNewMessage(it.getOrThrow())
                    it.isFailure -> listener.onError(it.exceptionOrNull() ?: BeaconException.Unknown())
                }
            }
        } catch (e: Exception) {
            listener.onError(e)
        }
    }
}

fun BeaconClient.respond(message: BeaconMessage.Response, callback: ResponseCallback) {
    sendScope {
        try {
            respond(message)
            callback.onSuccess()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

fun BeaconClient.Builder.storage(storage: BeaconCompatStorage): BeaconClient.Builder =
    storage(CompatStorageDecorator(storage))

private fun initScope(block: suspend () -> Unit) {
    jobScope(CoroutineName("beaconClient#init"), block)
}

private fun receiveScope(block: suspend () -> Unit) {
    jobScope(CoroutineName("beaconClient#receive"), block)
}

private fun sendScope(block: suspend () -> Unit) {
    jobScope(CoroutineName("beaconClient#send"), block)
}

private fun jobScope(coroutineName: CoroutineName, block: suspend () -> Unit) {
    CoroutineScope(coroutineName + Dispatchers.Default).launch {
        block()
    }
}