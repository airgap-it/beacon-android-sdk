package it.airgap.beaconsdk.internal.controller

import it.airgap.beaconsdk.data.beacon.Origin
import it.airgap.beaconsdk.data.beacon.Permission
import it.airgap.beaconsdk.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.internal.protocol.Protocol
import it.airgap.beaconsdk.internal.protocol.ProtocolRegistry
import it.airgap.beaconsdk.internal.storage.StorageManager
import it.airgap.beaconsdk.internal.utils.*
import it.airgap.beaconsdk.message.*

internal class MessageController(
    private val protocolRegistry: ProtocolRegistry,
    private val storageManager: StorageManager,
    private val accountUtils: AccountUtils,
) {
    private val pendingRequests: MutableMap<String, BeaconRequest> = mutableMapOf()

    // -- on incoming --

    suspend fun onIncomingMessage(origin: Origin, message: VersionedBeaconMessage): InternalResult<BeaconMessage> =
        tryResult {
            message.toBeaconMessage(origin, storageManager).also {
                when (it) {
                    is BeaconRequest -> onIncomingRequest(it)
                    else -> { /* no action */ }
                }
            }
        }

    private suspend fun onIncomingRequest(request: BeaconRequest) {
        pendingRequests[request.id] = request

        when (request) {
            is PermissionBeaconRequest -> onPermissionRequest(request)
            else -> { /* no action */ }
        }
    }

    private suspend fun onPermissionRequest(request: PermissionBeaconRequest) {
        storageManager.addAppMetadata(listOf(request.appMetadata))
    }

    // -- on outgoing --

    suspend fun onOutgoingMessage(
        beaconId: String,
        message: BeaconMessage,
        isTerminal: Boolean,
    ): InternalResult<Pair<Origin, VersionedBeaconMessage>> =
        tryResult {
            when (message) {
                is BeaconResponse -> onOutgoingResponse(message, isTerminal)
                else -> { /* no action */ }
            }

            val senderId = accountUtils.getSenderId(HexString.fromString(beaconId)).get()
            Pair(message.associatedOrigin, VersionedBeaconMessage.fromBeaconMessage(senderId, message))
        }

    private suspend fun onOutgoingResponse(response: BeaconResponse, isTerminal: Boolean) {
        val request = pendingRequests.get(response.id, remove = isTerminal) ?: failWithNoPendingRequest()
        when (response) {
            is PermissionBeaconResponse -> onPermissionResponse(response, request)
            else -> { /* no action */ }
        }
    }

    private suspend fun onPermissionResponse(response: PermissionBeaconResponse, request: BeaconRequest) {
        val publicKey = response.publicKey
        val address = protocolRegistry.get(Protocol.Type.Tezos).getAddressFromPublicKey(publicKey).get()
        val accountIdentifier = accountUtils.getAccountIdentifier(address, response.network).get()
        val appMetadata = storageManager.findAppMetadata { it.senderId == request.senderId }
            ?: failWithIllegalState("Granted permissions not saved, matching appMetadata could not be found.")

        val permissionInfo = Permission(
            accountIdentifier,
            address,
            response.network,
            response.scopes,
            accountUtils.getSenderId(HexString.fromString(request.origin.id)).get(),
            appMetadata,
            publicKey,
            connectedAt = currentTimestamp()
        )

        storageManager.addPermissions(listOf(permissionInfo))
    }

    private fun failWithNoPendingRequest(): Nothing = throw IllegalArgumentException("No matching request found")
}