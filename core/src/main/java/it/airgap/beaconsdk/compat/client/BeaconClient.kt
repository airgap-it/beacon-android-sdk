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

/**
 * Callback to be invoked when [build] finishes execution.
 */
public interface BuildCallback {
    public fun onSuccess(beaconClient: BeaconClient)
    public fun onError(error: Throwable)
}

/**
 * Callback to be invoked when a new [message][BeaconMessage] is received.
 */
public interface OnNewMessageListener {
    public fun onNewMessage(message: BeaconMessage)
    public fun onError(error: Throwable)
}

/**
 * Callback to be invoked when [respond] finishes execution.
 */
public interface ResponseCallback {
    public fun onSuccess()
    public fun onError(error: Throwable)
}

/**
 * Connects with known peers and listens for incoming messages with the given [listener].
 */
public fun BeaconClient.connect(listener: OnNewMessageListener) {
    receiveScope {
        try {
            connect().collect {
                when {
                    it.isSuccess -> listener.onNewMessage(it.getOrThrow())
                    it.isFailure -> listener.onError(it.exceptionOrNull() ?: BeaconException.Internal())
                }
            }
        } catch (e: Exception) {
            listener.onError(e)
        }
    }
}

/**
 * Sends the [response] in reply to a previously received request and calls the [callback] when finished.
 *
 * The method will fail if there is no pending request that matches the [response].
 */
public fun BeaconClient.respond(response: BeaconResponse, callback: ResponseCallback) {
    sendScope {
        try {
            respond(response)
            callback.onSuccess()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

/**
 * Builds a new instance of [BeaconClient] and calls the [callback] with the result.
 */
public fun BeaconClient.Builder.build(callback: BuildCallback) {
    buildScope {
        try {
            val beaconClient = build()
            callback.onSuccess(beaconClient)
        } catch (e: Exception) {
            callback.onError(e)
        }
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