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
import it.airgap.beaconsdk.internal.utils.Success
import it.airgap.beaconsdk.message.BeaconMessage
import it.airgap.beaconsdk.message.BeaconResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

public class BeaconClient internal constructor(
    public val name: String,
    public val beaconId: String,
    private val connectionController: ConnectionController,
    private val messageController: MessageController,
    private val storage: DecoratedExtendedStorage,
) {

    public fun connect(): Flow<Result<BeaconMessage>> {
        return connectionController.subscribe()
            .map { it.flatMapSuspend(messageController::onIncomingMessage) }
            .map {
                when (it) {
                    is Success -> Result.success(it.value)
                    is Failure -> Result.failure(it.error as? BeaconException ?: BeaconException.Internal(cause = it.error))
                }
            }
    }

    @Throws(BeaconException::class)
    public suspend fun respond(message: BeaconResponse) {
        try {
            val outgoingMessage = messageController.onOutgoingMessage(beaconId, message).value()
            connectionController.send(outgoingMessage).value()
        } catch (e: BeaconException) {
            throw e
        } catch (e: Exception) {
            throw BeaconException.Internal(cause = e)
        }
    }

    public suspend fun addPeers(vararg peers: P2pPeerInfo) {
        addPeers(peers.toList())
    }

    public suspend fun addPeers(peers: List<P2pPeerInfo>) {
        storage.addP2pPeers(peers)
    }

    public suspend fun getPeers(): List<P2pPeerInfo> =
        storage.getP2pPeers()

    public suspend fun removePeers(vararg peers: P2pPeerInfo) {
        removePeers(peers.toList())
    }

    public suspend fun removePeers(peers: List<P2pPeerInfo>) {
        storage.removeP2pPeers(peers)
    }

    public suspend fun getAppsMetadata(): List<AppMetadata> =
        storage.getAppsMetadata()

    public suspend fun getAppMetadataFor(senderId: String): AppMetadata? =
        storage.findAppMetadata { it.senderId == senderId }

    public suspend fun removeAppsMetadataFor(vararg senderIds: String) {
        storage.removeAppsMetadata { senderIds.contains(it.senderId) }
    }

    public suspend fun removeAppsMetadataFor(senderIds: List<String>) {
        storage.removeAppsMetadata { senderIds.contains(it.senderId) }
    }

    public suspend fun removeAppsMetadata(vararg appsMetadata: AppMetadata) {
        removeAppsMetadata(appsMetadata.toList())
    }

    public suspend fun removeAppsMetadata(appsMetadata: List<AppMetadata>) {
        storage.removeAppsMetadata(appsMetadata)
    }

    public suspend fun getPermissions(): List<PermissionInfo> =
        storage.getPermissions()

    public suspend fun getPermissionsFor(accountIdentifier: String): PermissionInfo? =
        storage.findPermission { it.accountIdentifier == accountIdentifier }

    public suspend fun removePermissionsFor(vararg accountIdentifiers: String) {
        storage.removePermissions { accountIdentifiers.contains(it.accountIdentifier) }
    }

    public suspend fun removePermissionsFor(accountIdentifiers: List<String>) {
        storage.removePermissions { accountIdentifiers.contains(it.accountIdentifier) }
    }

    public suspend fun removePermissions(vararg permissions: PermissionInfo) {
        removePermissions(permissions.toList())
    }

    public suspend fun removePermissions(permissions: List<PermissionInfo>) {
        storage.removePermissions(permissions)
    }

    public companion object {}

    public class Builder(private val name: String) {
        private var _matrixNodes: List<String> = emptyList()

        public fun matrixNodes(nodes: List<String>): Builder = apply { _matrixNodes = nodes }
        public fun matrixNodes(vararg nodes: String): Builder = apply { _matrixNodes = nodes.toList() }

        public suspend fun build(): BeaconClient {
            val beaconApp = BeaconApp.instance
            val storage = SharedPreferencesStorage.create(beaconApp.applicationContext)
            val matrixNodes = _matrixNodes.ifEmpty { BeaconConfig.defaultRelayServers }

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

public suspend fun BeaconClient(
    name: String,
    builderAction: BeaconClient.Builder.() -> Unit = {},
): BeaconClient = BeaconClient.Builder(name).apply(builderAction).build()

