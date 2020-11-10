package it.airgap.beaconsdk.internal.controller

import it.airgap.beaconsdk.data.beacon.Origin
import it.airgap.beaconsdk.data.beacon.PermissionInfo
import it.airgap.beaconsdk.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.internal.protocol.Protocol
import it.airgap.beaconsdk.internal.protocol.ProtocolRegistry
import it.airgap.beaconsdk.internal.storage.decorator.DecoratedExtendedStorage
import it.airgap.beaconsdk.internal.utils.*
import it.airgap.beaconsdk.message.BeaconMessage
import it.airgap.beaconsdk.message.PermissionBeaconRequest
import it.airgap.beaconsdk.message.PermissionBeaconResponse

internal class MessageController(
    private val protocolRegistry: ProtocolRegistry,
    private val storage: DecoratedExtendedStorage,
    private val accountUtils: AccountUtils,
) {
    private val pendingRequests: MutableList<VersionedBeaconMessage> = mutableListOf()

    suspend fun onIncomingMessage(origin: Origin, message: VersionedBeaconMessage): InternalResult<BeaconMessage> =
        tryResult {
            message.toBeaconMessage(origin, storage).also {
                when (it) {
                    is PermissionBeaconRequest -> onPermissionRequest(it)
                    else -> { /* no action */ }
                }
                pendingRequests.add(message)
            }
        }


    suspend fun onOutgoingMessage(
        senderId: String,
        message: BeaconMessage,
    ): InternalResult<VersionedBeaconMessage> =
        tryResult {
            val request = pendingRequests.pop { it.pairsWith(message) } ?: failWithNoPendingRequest()
            when (message) {
                is PermissionBeaconResponse -> onPermissionResponse(message, request)
                else -> { /* no action */ }
            }

            VersionedBeaconMessage.fromBeaconMessage(request.version, senderId, message)
        }

    private suspend fun onPermissionRequest(request: PermissionBeaconRequest) {
        storage.addAppMetadata(listOf(request.appMetadata))
    }

    private suspend fun onPermissionResponse(response: PermissionBeaconResponse, request: VersionedBeaconMessage) {
        val publicKey = response.publicKey
        val address = protocolRegistry.get(Protocol.Type.Tezos).getAddressFromPublicKey(publicKey).value()
        val accountIdentifier = accountUtils.getAccountIdentifier(address, response.network).value()
        val appMetadata = storage.findAppMetadata { request.comesFrom(it) }
            ?: failWithIllegalState("Granted permissions not saved, matching appMetadata could not be found.")

        val permissionInfo = PermissionInfo(
            accountIdentifier,
            address,
            response.network,
            response.scopes,
            appMetadata.senderId,
            appMetadata,
            publicKey,
            connectedAt = currentTimestamp()
        )

        storage.addPermissions(listOf(permissionInfo))
    }

    private fun failWithNoPendingRequest(): Nothing = throw IllegalArgumentException("No matching request found")
}