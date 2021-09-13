package it.airgap.beaconsdk.client.wallet

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.client.BeaconClient
import it.airgap.beaconsdk.core.data.beacon.AppMetadata
import it.airgap.beaconsdk.core.data.beacon.Connection
import it.airgap.beaconsdk.core.data.beacon.Peer
import it.airgap.beaconsdk.core.data.beacon.Permission
import it.airgap.beaconsdk.core.exception.BeaconException
import it.airgap.beaconsdk.core.internal.chain.Chain
import it.airgap.beaconsdk.core.internal.controller.ConnectionController
import it.airgap.beaconsdk.core.internal.controller.MessageController
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.storage.SecureStorage
import it.airgap.beaconsdk.core.internal.storage.Storage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.SharedPreferencesSecureStorage
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.SharedPreferencesStorage
import it.airgap.beaconsdk.core.internal.utils.applicationContext
import it.airgap.beaconsdk.core.internal.utils.beaconSdk
import it.airgap.beaconsdk.core.internal.utils.delegate.default
import it.airgap.beaconsdk.core.internal.utils.dependencyRegistry
import it.airgap.beaconsdk.core.internal.utils.mapException
import it.airgap.beaconsdk.core.message.AcknowledgeBeaconResponse
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconRequest
import it.airgap.beaconsdk.core.message.BeaconResponse
import kotlinx.coroutines.flow.Flow

/**
 * Asynchronous client that communicates with dApps.
 *
 * ### Receive requests
 *
 * To start receiving requests from connected dApps, call [connect] and subscribe to the returned [Flow], for example:
 *
 * ```
 * beaconWalletClient.connect()
 *     .mapNotNull { result -> result.getOrNull() }
 *     .collect { request ->
 *         println("Received $request")
 *     }
 * ```
 *
 * ### Respond to a request
 *
 * Use the ID [id][BeaconMessage.id] of the received message to construct a [response][BeaconResponse]
 * and send it back with [respond], for example:
 *
 * ```
 * // request = PermissionBeaconRequest(...)
 * val response = PermissionBeaconResponse(
 *     id = request.id,
 *     publicKey = "...",
 *     network = request.network,
 *     scopes = request.scopes,
 * )
 *
 * try {
 *     beaconWalletClient.respond(response)
 * } catch (e: Exception) {
 *     println("Failed to respond, error: $e")
 * }
 * ```
 *
 * ### Connect with a new dApp
 *
 * To connect with a new peer, create a new [peer][Peer] instance and register it in the [client][BeaconWalletClient]
 * using [addPeers], for example:
 *
 * ```
 * val peer = P2pPeerInfo(
 *     name = "Example dApp",
 *     publicKey = "...",
 *     relayServer = "...",
 * )
 *
 * beaconWalletClient.addPeers(peer)
 * ```
 *
 * ### Disconnect from a dApp
 *
 * To disconnect from a peer, unregister it in the [client][BeaconWalletClient] with [removePeers].
 */
public class BeaconWalletClient @RestrictTo(RestrictTo.Scope.LIBRARY) constructor(
    name: String,
    beaconId: String,
    connectionController: ConnectionController,
    messageController: MessageController,
    storageManager: StorageManager,
    crypto: Crypto,
) : BeaconClient<BeaconRequest>(name, beaconId, connectionController, messageController, storageManager, crypto) {

    /**
     * Sends the [response] in reply to a previously received request.
     *
     * @throws [BeaconException] if processing and sending the [response] failed.
     */
    @Throws(IllegalArgumentException::class, IllegalStateException::class, BeaconException::class)
    public suspend fun respond(response: BeaconResponse) {
        send(response, isTerminal = true).mapException { BeaconException.from(it) }.getOrThrow()
    }

    /**
     * Returns a list of stored app metadata.
     */
    public suspend fun getAppMetadata(): List<AppMetadata> =
        storageManager.getAppMetadata()

    /**
     * Returns the first app metadata that matches the specified [senderId]
     * or `null` if no such app metadata was found.
     */
    public suspend fun getAppMetadataFor(senderId: String): AppMetadata? =
        storageManager.findAppMetadata { it.senderId == senderId }

    /**
     * Removes app metadata that matches the specified [senderIds].
     */
    public suspend fun removeAppMetadataFor(vararg senderIds: String) {
        storageManager.removeAppMetadata { senderIds.contains(it.senderId) }
    }

    /**
     * Removes app metadata that matches the specified [senderIds].
     */
    public suspend fun removeAppMetadataFor(senderIds: List<String>) {
        storageManager.removeAppMetadata { senderIds.contains(it.senderId) }
    }

    /**
     * Removes the specified [appMetadata].
     */
    public suspend fun removeAppMetadata(vararg appMetadata: AppMetadata) {
        removeAppMetadata(appMetadata.toList())
    }

    /**
     * Removes all app metadata.
     */
    public suspend fun removeAllAppMetadata() {
        storageManager.removeAppMetadata()
    }

    /**
     * Removes the specified [appMetadata].
     */
    public suspend fun removeAppMetadata(appMetadata: List<AppMetadata>) {
        storageManager.removeAppMetadata(appMetadata)
    }

    /**
     * Returns a list of granted permissions.
     */
    public suspend fun getPermissions(): List<Permission> =
        storageManager.getPermissions()

    /**
     * Returns permissions granted for the specified [accountIdentifier].
     */
    public suspend fun getPermissionsFor(accountIdentifier: String): Permission? =
        storageManager.findPermission { it.accountIdentifier == accountIdentifier }

    /**
     * Removes permissions granted for the specified [accountIdentifiers].
     */
    public suspend fun removePermissionsFor(vararg accountIdentifiers: String) {
        storageManager.removePermissions { accountIdentifiers.contains(it.accountIdentifier) }
    }

    /**
     * Removes permissions granted for the specified [accountIdentifiers].
     */
    public suspend fun removePermissionsFor(accountIdentifiers: List<String>) {
        storageManager.removePermissions { accountIdentifiers.contains(it.accountIdentifier) }
    }

    /**
     * Removes the specified [permissions].
     */
    public suspend fun removePermissions(vararg permissions: Permission) {
        removePermissions(permissions.toList())
    }

    /**
     * Removes the specified [permissions].
     */
    public suspend fun removePermissions(permissions: List<Permission>) {
        storageManager.removePermissions(permissions)
    }

    /**
     * Removes all granted permissions.
     */
    public suspend fun removeAllPermissions() {
        storageManager.removePermissions()
    }

    protected override suspend fun processMessage(message: BeaconMessage): Result<Unit> =
        when (message) {
            is BeaconRequest -> acknowledge(message)
            else -> super.processMessage(message)
        }

    protected override suspend fun transformMessage(message: BeaconMessage): BeaconRequest? =
        when (message) {
            is BeaconRequest -> message
            else -> null
        }

    private suspend fun acknowledge(request: BeaconRequest): Result<Unit> {
        val acknowledgeResponse = AcknowledgeBeaconResponse.from(request, beaconId)
        return send(acknowledgeResponse, isTerminal = false)
    }

    public companion object {}

    /**
     * Asynchronous builder for [BeaconWalletClient].
     *
     * @constructor Creates a builder configured with the specified application [name] and [chains] that will be supported by the client.
     */
    public class Builder(private val name: String, private val chains: List<Chain.Factory<*>>) {

        /**
         * A URL to the application's website.
         */
        public var appUrl: String? = null

        /**
         * A URL to the application's icon.
         */
        public var iconUrl: String? = null

        /**
         * Connection types that will be supported by the configured client.
         */
        private var connections: MutableList<Connection> = mutableListOf()

        /**
         * Registers connections that should be supported by the configured client.
         */
        public fun addConnections(vararg connections: Connection): Builder = apply {
            this.connections.addAll(connections.toList())
        }

        /**
         * An optional external implementation of [Storage]. If not provided, an internal implementation will be used.
         */
        public var storage: Storage by default { SharedPreferencesStorage.create(applicationContext) }

        /**
         * An optional external implementation of [SecureStorage]. If not provided, an internal implementation will be used.
         */
        public var secureStorage: SecureStorage by default { SharedPreferencesSecureStorage.create(applicationContext) }

        /**
         * Builds a new instance of [BeaconWalletClient].
         */
        public suspend fun build(): BeaconWalletClient {
            beaconSdk.init(name, appUrl, iconUrl, chains, storage, secureStorage)

            with(dependencyRegistry) {
                return BeaconWalletClient(
                    name,
                    beaconSdk.beaconId,
                    connectionController(connections),
                    messageController,
                    storageManager,
                    crypto,
                )
            }
        }
    }
}

/**
 * Creates a new instance of [BeaconWalletClient] with the specified application [name], supporting registered [chains] and configured with [builderAction].
 *
 * @see [BeaconWalletClient.Builder]
 */
public suspend fun BeaconWalletClient(
    name: String,
    chains: List<Chain.Factory<*>>,
    builderAction: BeaconWalletClient.Builder.() -> Unit = {},
): BeaconWalletClient = BeaconWalletClient.Builder(name, chains).apply(builderAction).build()

