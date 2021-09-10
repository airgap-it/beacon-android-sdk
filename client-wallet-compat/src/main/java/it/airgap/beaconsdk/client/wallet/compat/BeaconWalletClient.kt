package it.airgap.beaconsdk.client.wallet.compat

import it.airgap.beaconsdk.client.wallet.BeaconWalletClient
import it.airgap.beaconsdk.core.exception.BeaconException
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

/**
 * Callback to be invoked when [build] finishes execution.
 */
public interface BuildCallback {
    public fun onSuccess(beaconClient: BeaconWalletClient)
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
public fun BeaconWalletClient.connect(listener: OnNewMessageListener) {
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
public fun BeaconWalletClient.disconnect(listener: OnNewMessageListener) {
    BeaconCompat.removeListener(listener)
}

/**
 * Cancels all listeners and callbacks.
 */
public fun BeaconWalletClient.stop() {
    BeaconCompat.cancelScopes()
}

/**
 * Sends the [response] in reply to a previously received request and calls the [callback] when finished.
 *
 * The method will fail if there is no pending request that matches the [response].
 */
public fun BeaconWalletClient.respond(response: BeaconResponse, callback: ResponseCallback) {
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
 * Builds a new instance of [BeaconWalletClient] and calls the [callback] with the result.
 */
public fun BeaconWalletClient.Builder.build(callback: BuildCallback) {
    BeaconCompat.buildScope {
        try {
            val beaconWalletClient = build()
            callback.onSuccess(beaconWalletClient)
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}