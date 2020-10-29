package it.airgap.beaconsdk.compat.client

import it.airgap.beaconsdk.client.BeaconClient
import it.airgap.beaconsdk.exception.BeaconException
import it.airgap.beaconsdk.message.BeaconMessage
import it.airgap.beaconsdk.message.BeaconResponse
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

public interface BuildCallback {
    public fun onSuccess(beaconClient: BeaconClient)
}

public interface OnNewMessageListener {
    public fun onNewMessage(message: BeaconMessage)
    public fun onError(error: Throwable)
}

public interface ResponseCallback {
    public fun onSuccess()
    public fun onError(error: Throwable)
}

public fun BeaconClient.connect(listener: OnNewMessageListener) {
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

public fun BeaconClient.respond(message: BeaconResponse, callback: ResponseCallback) {
    sendScope {
        try {
            respond(message)
            callback.onSuccess()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

public fun BeaconClient.Builder.build(callback: BuildCallback) {
    buildScope {
        val beaconClient = build()
        callback.onSuccess(beaconClient)
    }
}


private fun receiveScope(block: suspend () -> Unit) {
    jobScope(CoroutineName("BeaconClient#receive"), block)
}

private fun sendScope(block: suspend () -> Unit) {
    jobScope(CoroutineName("BeaconClient#send"), block)
}

private fun buildScope(block: suspend () -> Unit) {
    jobScope(CoroutineName("BeaconClient.Builder#build"), block)
}

private fun jobScope(coroutineName: CoroutineName, block: suspend () -> Unit) {
    CoroutineScope(coroutineName + Dispatchers.Default).launch {
        block()
    }
}