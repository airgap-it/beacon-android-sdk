package it.airgap.beaconsdk.compat.client

import it.airgap.beaconsdk.client.BeaconClient
import it.airgap.beaconsdk.exception.BeaconException
import it.airgap.beaconsdk.message.BeaconMessage
import it.airgap.beaconsdk.message.BeaconResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

/**
 * Callback to be invoked when [build] finishes execution.
 */
public interface BuildCallback {
    public fun onSuccess(beaconClient: BeaconClient)
    public fun onError(error: Throwable)
    public fun onCancel() {}
}

/**
 * Callback to be invoked when a new [message][BeaconMessage] is received.
 */
public interface OnNewMessageListener {
    public fun onNewMessage(message: BeaconMessage)
    public fun onError(error: Throwable)
    public fun onCancel() {}
}

/**
 * Callback to be invoked when [respond] finishes execution.
 */
public interface ResponseCallback {
    public fun onSuccess()
    public fun onError(error: Throwable)
    public fun onCancel() {}
}

/**
 * Connects with known peers and listens for incoming messages with the given [listener].
 */
public fun BeaconClient.connect(listener: OnNewMessageListener) {
    val listenerId = BeaconCompat.addListener(listener)
    BeaconCompat.receiveScope {
        try {
            connect().collect { result ->
                val listener = BeaconCompat.listeners[listenerId] ?: return@collect
                result
                    .onSuccess { listener.onNewMessage(it) }
                    .onFailure { listener.onError(BeaconException.from(it)) }
            }
        } catch (e: CancellationException) {
            listener.onCancel()
        } catch (e: Exception) {
            listener.onError(e)
        }
    }
}

/**
 * Removes the given [listener] from the set of listeners receiving updates on incoming messages.
 */
public fun BeaconClient.disconnect(listener: OnNewMessageListener) {
    BeaconCompat.removeListener(listener)
}

/**
 * Cancels all listeners and callbacks.
 */
public fun BeaconClient.stop() {
    BeaconCompat.cancelScopes()
}

/**
 * Sends the [response] in reply to a previously received request and calls the [callback] when finished.
 *
 * The method will fail if there is no pending request that matches the [response].
 */
public fun BeaconClient.respond(response: BeaconResponse, callback: ResponseCallback) {
    BeaconCompat.sendScope {
        try {
            respond(response)
            callback.onSuccess()
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

/**
 * Builds a new instance of [BeaconClient] and calls the [callback] with the result.
 */
public fun BeaconClient.Builder.build(callback: BuildCallback) {
    BeaconCompat.buildScope {
        try {
            val beaconClient = build()
            callback.onSuccess(beaconClient)
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}