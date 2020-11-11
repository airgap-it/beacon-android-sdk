package it.airgap.beaconsdk.client

import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.P2pPeerInfo
import it.airgap.beaconsdk.data.beacon.PermissionInfo
import it.airgap.beaconsdk.exception.BeaconException
import it.airgap.beaconsdk.internal.BeaconApp
import it.airgap.beaconsdk.internal.BeaconConfig
import it.airgap.beaconsdk.internal.controller.ConnectionController
import it.airgap.beaconsdk.internal.controller.MessageController
import it.airgap.beaconsdk.internal.storage.SharedPreferencesStorage
import it.airgap.beaconsdk.internal.storage.decorator.DecoratedExtendedStorage
import it.airgap.beaconsdk.internal.transport.Transport
import it.airgap.beaconsdk.internal.utils.Failure
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.Success
import it.airgap.beaconsdk.message.BeaconMessage
import it.airgap.beaconsdk.message.BeaconRequest
import it.airgap.beaconsdk.message.BeaconResponse
import it.airgap.beaconsdk.message.PermissionBeaconResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

/**
 * Asynchronous client that communicates with dApps.
 *
 * ### Receive requests
 *
 * To start receiving requests from connected dApps, call [connect] and subscribe to the returned [Flow], for example:
 *
 * ```
 * beaconClient.connect()
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
 *     beaconClient.respond(response)
 * } catch (e: Exception) {
 *     println("Failed to respond, error: $e")
 * }
 * ```
 *
 * ### Connect with a new dApp
 *
 * To connect with a new peer, create a new [peer][P2pPeerInfo] instance and register it in the [client][BeaconClient]
 * using [addPeers], for example:
 *
 * ```
 * val peer = P2pPeerInfo(
 *     name = "Example dApp",
 *     publicKey = "...",
 *     relayServer = "...",
 * )
 *
 * beaconClient.addPeers(peer)
 * ```
 *
 * ### Disconnect from a dApp
 *
 * To disconnect from a peer, unregister it in the [client][BeaconClient] with [removePeers].
 */
public class BeaconClient internal constructor(
    public val name: String,
    public val beaconId: String,
    private val connectionController: ConnectionController,
    private val messageController: MessageController,
    private val storage: DecoratedExtendedStorage,
) {

    /**
     * Connects with known peers and subscribes to incoming messages
     * returning a [Flow] of received [BeaconRequest] instances or occurred errors
     * represented as a [Result].
     */
    public fun connect(): Flow<Result<BeaconRequest>> =
        connectionController.subscribe()
            .map { result ->
                result.flatMapSuspend { messageController.onIncomingMessage(it.origin, it.content) }
            }
            .filterIsInstance<InternalResult<BeaconRequest>>()
            .map {
                when (it) {
                    is Success -> Result.success(it.value)
                    is Failure -> Result.failure(it.error as? BeaconException ?: BeaconException(cause = it.error))
                }
            }

    /**
     * Sends the [response] in reply to a previously received request.
     *
     * @throws [IllegalArgumentException] if no pending request that matches the [response] was found.
     * @throws [IllegalStateException] on [PermissionBeaconResponse] if the granted permissions could not be saved.
     * @throws [BeaconException] if sending the [response] failed.
     */
    @Throws(IllegalArgumentException::class, IllegalStateException::class, BeaconException::class)
    public suspend fun respond(response: BeaconResponse) {
        val outgoingMessage = messageController.onOutgoingMessage(beaconId, response).value()
        connectionController.send(outgoingMessage).mapError { BeaconException(cause = it) }.value()
    }

    /**
     * Adds new P2P [peers].
     *
     * The new peers will be persisted and subscribed.
     */
    public suspend fun addPeers(vararg peers: P2pPeerInfo) {
        addPeers(peers.toList())
    }

    /**
     * Adds new P2P [peers].
     *
     * The new peers will be persisted and subscribed.
     */
    public suspend fun addPeers(peers: List<P2pPeerInfo>) {
        storage.addP2pPeers(peers)
    }

    /**
     * Returns a list of known P2P peers.
     */
    public suspend fun getPeers(): List<P2pPeerInfo> =
        storage.getP2pPeers()

    /**
     * Removes the specified P2P [peers].
     *
     * The removed peers will be unsubscribed.
     */
    public suspend fun removePeers(vararg peers: P2pPeerInfo) {
        removePeers(peers.toList())
    }

    /**
     * Removes the specified P2P [peers].
     *
     * The removed peers will be unsubscribed.
     */
    public suspend fun removePeers(peers: List<P2pPeerInfo>) {
        storage.removeP2pPeers(peers)
    }

    /**
     * Removes all known P2P peers.
     *
     * All peers will be unsubscribed.
     */
    public suspend fun removeAllPeers() {
        storage.removeP2pPeers()
    }

    /**
     * Returns a list of stored app metadata.
     */
    public suspend fun getAppMetadata(): List<AppMetadata> =
        storage.getAppMetadata()

    /**
     * Returns the first app metadata that matches the specified [senderId]
     * or `null` if no such app metadata was found.
     */
    public suspend fun getAppMetadataFor(senderId: String): AppMetadata? =
        storage.findAppMetadata { it.senderId == senderId }

    /**
     * Removes app metadata that matches the specified [senderIds].
     */
    public suspend fun removeAppMetadataFor(vararg senderIds: String) {
        storage.removeAppMetadata { senderIds.contains(it.senderId) }
    }

    /**
     * Removes app metadata that matches the specified [senderIds].
     */
    public suspend fun removeAppMetadataFor(senderIds: List<String>) {
        storage.removeAppMetadata { senderIds.contains(it.senderId) }
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
        storage.removeAppMetadata()
    }

    /**
     * Removes the specified [appMetadata].
     */
    public suspend fun removeAppMetadata(appMetadata: List<AppMetadata>) {
        storage.removeAppMetadata(appMetadata)
    }

    /**
     * Returns a list of granted permissions.
     */
    public suspend fun getPermissions(): List<PermissionInfo> =
        storage.getPermissions()

    /**
     * Returns a list of permissions granted for the specified [accountIdentifier].
     */
    public suspend fun getPermissionsFor(accountIdentifier: String): PermissionInfo? =
        storage.findPermission { it.accountIdentifier == accountIdentifier }

    /**
     * Removes permissions granted for the specified [accountIdentifiers].
     */
    public suspend fun removePermissionsFor(vararg accountIdentifiers: String) {
        storage.removePermissions { accountIdentifiers.contains(it.accountIdentifier) }
    }

    /**
     * Removes permissions granted for the specified [accountIdentifiers].
     */
    public suspend fun removePermissionsFor(accountIdentifiers: List<String>) {
        storage.removePermissions { accountIdentifiers.contains(it.accountIdentifier) }
    }

    /**
     * Removes the specified [permissions].
     */
    public suspend fun removePermissions(vararg permissions: PermissionInfo) {
        removePermissions(permissions.toList())
    }

    /**
     * Removes the specified [permissions].
     */
    public suspend fun removePermissions(permissions: List<PermissionInfo>) {
        storage.removePermissions(permissions)
    }

    /**
     * Removes all granted permissions.
     */
    public suspend fun removeAllPermissions() {
        storage.removePermissions()
    }

    public companion object {}

    /**
     * Asynchronous builder for [BeaconClient].
     *
     * @constructor Creates a builder configured with the specified application [name] and empty [matrixNodes].
     */
    public class Builder(private val name: String) {

        /**
         * Custom Matrix nodes.
         *
         * The nodes will be used to establish a P2P connection. If no custom nodes are set,
         * the default value provided in [BeaconConfig] will be used.
         */
        public var matrixNodes: List<String> = emptyList()

        /**
         * Builds a new instance of [BeaconClient].
         */
        public suspend fun build(): BeaconClient {
            val beaconApp = BeaconApp.instance
            val storage = SharedPreferencesStorage.create(beaconApp.applicationContext)
            val matrixNodes = matrixNodes.toList().ifEmpty { BeaconConfig.defaultRelayServers }

            beaconApp.init(name, matrixNodes, storage)

            with(beaconApp.dependencyRegistry) {
                return BeaconClient(
                    name,
                    beaconApp.beaconId,
                    connectionController(Transport.Type.P2P),
                    messageController,
                    extendedStorage,
                )
            }
        }
    }
}

/**
 * Creates a new instance of [BeaconClient] with the specified application [name] and configured with [builderAction].
 *
 * @see [BeaconClient.Builder]
 */
public suspend fun BeaconClient(
    name: String,
    builderAction: BeaconClient.Builder.() -> Unit = {},
): BeaconClient = BeaconClient.Builder(name).apply(builderAction).build()

