package it.airgap.beaconsdk.client.wallet.compat

import it.airgap.beaconsdk.client.wallet.BeaconWalletClient
import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.Peer
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.exception.BeaconException
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect

// -- interfaces --

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
 * Callback to be invoked when a value is read from the storage.
 */
public interface GetCallback<T> {
    public fun onSuccess(value: T)
    public fun onError(error: Throwable)
    public fun onCancel() {}
}

/**
 * Callback to be invoked when a value is set in the storage.
 */
public interface SetCallback {
    public fun onSuccess()
    public fun onError(error: Throwable)
    public fun onCancel() {}
}

// -- connect --

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

// -- respond --

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

// -- build --

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

// -- storage --

/**
 * Adds new [peers] and calls the [callback] when finished.
 *
 * The new peers will be persisted and subscribed.
 */
public fun BeaconWalletClient.addPeers(vararg peers: Peer, callback: SetCallback) {
    BeaconCompat.storageScope {
        try {
            addPeers(peers.toList())
            callback.onSuccess()
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

/**
 * Adds new [peers] and calls the [callback] when finished.
 *
 * The new peers will be persisted and subscribed.
 */
public fun BeaconWalletClient.addPeers(peers: List<Peer>, callback: SetCallback) {
    BeaconCompat.storageScope {
        try {
            addPeers(peers)
            callback.onSuccess()
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

/**
 * Calls the [callback] with a list of known peers.
 */
public fun BeaconWalletClient.getPeers(callback: GetCallback<List<Peer>>) {
    BeaconCompat.storageScope {
        try {
            val peers = getPeers()
            callback.onSuccess(peers)
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

/**
 * Removes the specified [peers] and calls the [callback] when finished.
 *
 * The removed peers will be unsubscribed.
 */
public fun BeaconWalletClient.removePeers(vararg peers: Peer, callback: SetCallback) {
    BeaconCompat.storageScope {
        try {
            removePeers(peers.toList())
            callback.onSuccess()
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

/**
 * Removes the specified [peers] and calls the [callback] when finished.
 *
 * The removed peers will be unsubscribed.
 */
public fun BeaconWalletClient.removePeers(peers: List<Peer>, callback: SetCallback) {
    BeaconCompat.storageScope {
        try {
            removePeers(peers)
            callback.onSuccess()
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

/**
 * Removes all known peers and calls the [callback] when finished.
 *
 * All peers will be unsubscribed.
 */
public fun BeaconWalletClient.removeAllPeers(callback: SetCallback) {
    BeaconCompat.storageScope {
        try {
            removeAllPeers()
            callback.onSuccess()
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

/**
 * Calls the [callback] with a list of stored app metadata.
 */
public fun BeaconWalletClient.getAppMetadata(callback: GetCallback<List<AppMetadata>>) {
    BeaconCompat.storageScope {
        try {
            val appMetadata = getAppMetadata()
            callback.onSuccess(appMetadata)
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

/**
 * Calls the [callback] with the first app metadata that matches the specified [senderId]
 * or `null` if no such app metadata was found.
 */
public fun BeaconWalletClient.getAppMetadataFor(senderId: String, callback: GetCallback<AppMetadata?>) {
    BeaconCompat.storageScope {
        try {
            val appMetadata = getAppMetadataFor(senderId)
            callback.onSuccess(appMetadata)
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

/**
 * Removes app metadata that matches the specified [senderIds] and calls the [callback] when finished.
 */
public fun BeaconWalletClient.removeAppMetadataFor(vararg senderIds: String, callback: SetCallback) {
    BeaconCompat.storageScope {
        try {
            removeAppMetadataFor(senderIds.toList())
            callback.onSuccess()
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

/**
 * Removes app metadata that matches the specified [senderIds] and calls the [callback] when finished.
 */
public fun BeaconWalletClient.removeAppMetadataFor(senderIds: List<String>, callback: SetCallback) {
    BeaconCompat.storageScope {
        try {
            removeAppMetadataFor(senderIds)
            callback.onSuccess()
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

/**
 * Removes the specified [appMetadata] and calls the [callback] when finished.
 */
public fun BeaconWalletClient.removeAppMetadata(vararg appMetadata: AppMetadata, callback: SetCallback) {
    BeaconCompat.storageScope {
        try {
            removeAppMetadata(appMetadata.toList())
            callback.onSuccess()
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

/**
 * Removes the specified [appMetadata] and calls the [callback] when finished.
 */
public fun BeaconWalletClient.removeAppMetadata(appMetadata: List<AppMetadata>, callback: SetCallback) {
    BeaconCompat.storageScope {
        try {
            removeAppMetadata(appMetadata)
            callback.onSuccess()
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

/**
 * Removes all app metadata and calls the [callback] when finished.
 */
public fun BeaconWalletClient.removeAllAppMetadata(callback: SetCallback) {
    BeaconCompat.storageScope {
        try {
            removeAllAppMetadata()
            callback.onSuccess()
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

/**
 * Calls the [callback] with a list of granted permissions.
 */
public fun BeaconWalletClient.getPermissions(callback: GetCallback<List<Permission>>) {
    BeaconCompat.storageScope {
        try {
            val permissions = getPermissions()
            callback.onSuccess(permissions)
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

/**
 * Calls the [callback] with the first permission granted for the specified [accountIdentifier]
 * or `null` if no such permission was found.
 */
public fun BeaconWalletClient.getPermissionsFor(accountIdentifier: String, callback: GetCallback<Permission?>) {
    BeaconCompat.storageScope {
        try {
            val permission = getPermissionsFor(accountIdentifier)
            callback.onSuccess(permission)
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

/**
 * Removes permissions granted for the specified [accountIdentifiers] and calls the [callback] when finished.
 */
public fun BeaconWalletClient.removePermissionsFor(vararg accountIdentifiers: String, callback: SetCallback) {
    BeaconCompat.storageScope {
        try {
            removePermissionsFor(accountIdentifiers.toList())
            callback.onSuccess()
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

/**
 * Removes permissions granted for the specified [accountIdentifiers] and calls the [callback] when finished.
 */
public fun BeaconWalletClient.removePermissionsFor(accountIdentifiers: List<String>, callback: SetCallback) {
    BeaconCompat.storageScope {
        try {
            removePermissionsFor(accountIdentifiers)
            callback.onSuccess()
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

/**
 * Removes the specified [permissions] and calls the [callback] when finished.
 */
public fun BeaconWalletClient.removePermissions(vararg permissions: Permission, callback: SetCallback) {
    BeaconCompat.storageScope {
        try {
            removePermissions(permissions.toList())
            callback.onSuccess()
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

/**
 * Removes the specified [permissions] and calls the [callback] when finished.
 */
public fun BeaconWalletClient.removePermissions(permissions: List<Permission>, callback: SetCallback) {
    BeaconCompat.storageScope {
        try {
            removePermissions(permissions)
            callback.onSuccess()
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

/**
 * Removes all granted permissions.and calls the [callback] when finished.
 */
public fun BeaconWalletClient.removeAllPermissions(callback: SetCallback) {
    BeaconCompat.storageScope {
        try {
            removeAllPermissions()
            callback.onSuccess()
        } catch (e: CancellationException) {
            callback.onCancel()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}