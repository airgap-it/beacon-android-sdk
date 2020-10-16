package it.airgap.beaconsdk.internal.controller

import it.airgap.beaconsdk.data.permission.PermissionInfo
import it.airgap.beaconsdk.internal.message.beaconmessage.ApiBeaconMessage
import it.airgap.beaconsdk.internal.protocol.Protocol
import it.airgap.beaconsdk.internal.protocol.ProtocolRegistry
import it.airgap.beaconsdk.compat.storage.ExtendedStorage
import it.airgap.beaconsdk.internal.utils.AccountUtils
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.currentTimestamp
import it.airgap.beaconsdk.internal.utils.pop
import it.airgap.beaconsdk.internal.utils.suspendTryInternal

internal class MessageController(
    private val protocolRegistry: ProtocolRegistry,
    private val storage: ExtendedStorage,
    private val accountUtils: AccountUtils,
) {
    private val pendingRequests: MutableList<ApiBeaconMessage.Request> = mutableListOf()

    suspend fun onRequest(request: ApiBeaconMessage.Request): InternalResult<Unit> =
        suspendTryInternal {
            pendingRequests.add(request)
            when (request) {
                is ApiBeaconMessage.Request.Permission -> onPermissionRequest(request)
                is ApiBeaconMessage.Request.Operation -> onOperationRequest(request)
                is ApiBeaconMessage.Request.SignPayload -> onSignPayloadRequest(request)
                is ApiBeaconMessage.Request.Broadcast -> onBroadcastRequest(request)
            }
        }


    suspend fun onResponse(response: ApiBeaconMessage.Response): InternalResult<Unit> =
        suspendTryInternal {
            val request = pendingRequests.pop { it.id == response.id } ?: failWithNoPendingRequest()
            when (response) {
                is ApiBeaconMessage.Response.Permission -> onPermissionResponse(response, request)
                is ApiBeaconMessage.Response.Operation -> onOperationResponse(response)
                is ApiBeaconMessage.Response.SignPayload -> onSignPayloadResponse(response)
                is ApiBeaconMessage.Response.Broadcast -> onBroadcastResponse(response)
            }
        }

    private suspend fun onPermissionRequest(request: ApiBeaconMessage.Request.Permission) {
        storage.addAppsMetadata(request.appMetadata)
    }

    private fun onOperationRequest(request: ApiBeaconMessage.Request.Operation) = Unit
    private fun onSignPayloadRequest(request: ApiBeaconMessage.Request.SignPayload) = Unit
    private fun onBroadcastRequest(request: ApiBeaconMessage.Request.Broadcast) = Unit

    private suspend fun onPermissionResponse(
        response: ApiBeaconMessage.Response.Permission,
        request: ApiBeaconMessage.Request
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

    private fun onOperationResponse(response: ApiBeaconMessage.Response.Operation) = Unit
    private fun onSignPayloadResponse(response: ApiBeaconMessage.Response.SignPayload) = Unit
    private fun onBroadcastResponse(response: ApiBeaconMessage.Response.Broadcast) = Unit

    private fun failWithNoAppMetadata(): Nothing = throw IllegalArgumentException("No matching app metadata found")
    private fun failWithNoPendingRequest(): Nothing = throw IllegalArgumentException("No matching request found")
}