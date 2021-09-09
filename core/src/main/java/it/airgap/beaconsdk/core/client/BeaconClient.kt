package it.airgap.beaconsdk.core.client

import it.airgap.beaconsdk.core.data.beacon.*
import it.airgap.beaconsdk.core.exception.BeaconException
import it.airgap.beaconsdk.core.internal.chain.Chain
import it.airgap.beaconsdk.core.internal.controller.ConnectionController
import it.airgap.beaconsdk.core.internal.controller.MessageController
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.message.BeaconConnectionMessage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.SharedPreferencesSecureStorage
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.SharedPreferencesStorage
import it.airgap.beaconsdk.core.internal.utils.*
import it.airgap.beaconsdk.core.message.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach

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
 * To connect with a new peer, create a new [peer][Peer] instance and register it in the [client][BeaconClient]
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
    private val storageManager: StorageManager,
    private val crypto: Crypto,
) {

    /**
     * Connects with known peers and subscribes to incoming messages
     * returning a [Flow] of received [BeaconRequest] instances or occurred errors
     * represented as a [Result].
     */
    public fun connect(): Flow<Result<BeaconRequest>> =
        connectionController.subscribe()
            .map { result -> result.flatMap { messageController.onIncomingMessage(it.origin, it.content) } }
            .onEach { result -> result.getOrNull()?.let { processMessage(it) } }
            .mapNotNull { result ->
                result.fold(
                    onSuccess = {
                        when (it) {
                            is BeaconRequest -> Result.success(it)
                            else -> null
                        }
                    },
                    onFailure = { Result.failure(BeaconException.from(it)) }
                )
            }

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
     * Adds new [peers].
     *
     * The new peers will be persisted and subscribed.
     */
    public suspend fun addPeers(vararg peers: Peer) {
        addPeers(peers.toList())
    }

    /**
     * Adds new [peers].
     *
     * The new peers will be persisted and subscribed.
     */
    public suspend fun addPeers(peers: List<Peer>) {
        storageManager.addPeers(peers)
    }

    /**
     * Returns a list of known peers.
     */
    public suspend fun getPeers(): List<Peer> =
        storageManager.getPeers()

    /**
     * Removes the specified [peers].
     *
     * The removed peers will be unsubscribed.
     */
    public suspend fun removePeers(vararg peers: Peer) {
        removePeers(peers.toList())
    }

    /**
     * Removes the specified [peers].
     *
     * The removed peers will be unsubscribed.
     */
    public suspend fun removePeers(peers: List<Peer>) {
        storageManager.removePeers(peers)
        peers.launchForEach { disconnect(it) }
    }

    /**
     * Removes all known peers.
     *
     * All peers will be unsubscribed.
     */
    public suspend fun removeAllPeers() {
        val peers = storageManager.getPeers()
        storageManager.removePeers()
        peers.launchForEach { disconnect(it) }
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

    private suspend fun processMessage(message: BeaconMessage): Result<Unit> =
        when (message) {
            is BeaconRequest -> acknowledge(message)
            is DisconnectBeaconMessage -> {
                removePeer(message.origin.id)
                Result.success()
            }
            else -> {
                /* no action */
                Result.success()
            }
        }

    private suspend fun removePeer(publicKey: String) {
        storageManager.removePeers { it.publicKey == publicKey }
    }

    private suspend fun acknowledge(request: BeaconRequest): Result<Unit> {
        val acknowledgeResponse = AcknowledgeBeaconResponse.from(request, beaconId)
        return send(acknowledgeResponse, isTerminal = false)
    }

    private suspend fun disconnect(peer: Peer): Result<Unit> =
        runCatchingFlat {
            val message = DisconnectBeaconMessage(crypto.guid().getOrThrow(), beaconId, peer.version, Origin.forPeer(peer))
            send(message, isTerminal = true)
        }

    private suspend fun send(message: BeaconMessage, isTerminal: Boolean): Result<Unit> =
        messageController.onOutgoingMessage(beaconId, message, isTerminal)
            .flatMap { connectionController.send(BeaconConnectionMessage(it)) }

    public companion object {}

    /**
     * Asynchronous builder for [BeaconClient].
     *
     * @constructor Creates a builder configured with the specified application [name] and list of [connections] with [P2P] included by default.
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
        public var connections: List<Connection> = listOf(P2P())

        /**
         * Builds a new instance of [BeaconClient].
         */
        public suspend fun build(): BeaconClient {
            val storage = SharedPreferencesStorage.create(applicationContext)
            val secureStorage = SharedPreferencesSecureStorage.create(applicationContext)

            beaconSdk.init(name, appUrl, iconUrl, chains, storage, secureStorage)

            with(dependencyRegistry) {
                return BeaconClient(
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
 * Creates a new instance of [BeaconClient] with the specified application [name] and configured with [builderAction].
 *
 * @see [BeaconClient.Builder]
 */
public suspend fun BeaconClient(
    name: String,
    chains: List<Chain.Factory<*>>,
    builderAction: BeaconClient.Builder.() -> Unit = {},
): BeaconClient = BeaconClient.Builder(name, chains).apply(builderAction).build()

