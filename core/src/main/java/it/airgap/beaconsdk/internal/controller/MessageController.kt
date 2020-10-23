package it.airgap.beaconsdk.internal.controller

import it.airgap.beaconsdk.data.permission.PermissionInfo
import it.airgap.beaconsdk.internal.protocol.Protocol
import it.airgap.beaconsdk.internal.protocol.ProtocolRegistry
import it.airgap.beaconsdk.internal.storage.ExtendedStorage
import it.airgap.beaconsdk.internal.utils.*
import it.airgap.beaconsdk.message.BeaconMessage

internal class MessageController(
    private val protocolRegistry: ProtocolRegistry,
    private val storage: ExtendedStorage,
    private val accountUtils: AccountUtils,
) {
    private val pendingRequests: MutableList<BeaconMessage.Request> = mutableListOf()

    suspend fun onRequest(request: BeaconMessage.Request): InternalResult<Unit> =
        tryResult {
            pendingRequests.add(request)
            when (request) {
                is BeaconMessage.Request.Permission -> onPermissionRequest(request)
                is BeaconMessage.Request.Operation -> onOperationRequest(request)
                is BeaconMessage.Request.SignPayload -> onSignPayloadRequest(request)
                is BeaconMessage.Request.Broadcast -> onBroadcastRequest(request)
            }
        }


    suspend fun onResponse(response: BeaconMessage.Response): InternalResult<Unit> =
        tryResult {
            val request = pendingRequests.pop { it.id == response.id } ?: failWithNoPendingRequest()
            when (response) {
                is BeaconMessage.Response.Permission -> onPermissionResponse(response, request)
                is BeaconMessage.Response.Operation -> onOperationResponse(response)
                is BeaconMessage.Response.SignPayload -> onSignPayloadResponse(response)
                is BeaconMessage.Response.Broadcast -> onBroadcastResponse(response)
            }
        }

    private suspend fun onPermissionRequest(request: BeaconMessage.Request.Permission) {
        request.appMetadata?.let { storage.addAppsMetadata(it) }
    }

    private fun onOperationRequest(request: BeaconMessage.Request.Operation) = Unit
    private fun onSignPayloadRequest(request: BeaconMessage.Request.SignPayload) = Unit
    private fun onBroadcastRequest(request: BeaconMessage.Request.Broadcast) = Unit

    private suspend fun onPermissionResponse(
        response: BeaconMessage.Response.Permission,
        request: BeaconMessage.Request
    ) {
        val publicKey = response.publicKey
        val address = protocolRegistry.get(Protocol.Type.Tezos).getAddressFromPublicKey(publicKey).getOrThrow()
        val accountIdentifier = accountUtils.getAccountIdentifier(address, response.network).getOrThrow()
        val appMetadata = storage.findAppMetadata { it.senderId == request.senderId } ?: failWithNoAppMetadata()

        val permissionInfo = PermissionInfo(
            accountIdentifier,
            address,
            response.network,
            response.scopes,
            request.senderId,
            appMetadata,
            "",
            publicKey,
            connectedAt = currentTimestamp()
        )

        storage.addPermissions(permissionInfo)
    }

    private fun onOperationResponse(response: BeaconMessage.Response.Operation) = Unit
    private fun onSignPayloadResponse(response: BeaconMessage.Response.SignPayload) = Unit
    private fun onBroadcastResponse(response: BeaconMessage.Response.Broadcast) = Unit

    private fun failWithNoAppMetadata(): Nothing = throw IllegalArgumentException("No matching app metadata found")
    private fun failWithNoPendingRequest(): Nothing = throw IllegalArgumentException("No matching request found")
}