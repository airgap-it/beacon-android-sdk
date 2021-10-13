package it.airgap.beaconsdk.core.internal.controller

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.*
import it.airgap.beaconsdk.core.message.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MessageController internal constructor(
    private val blockchainRegistry: BlockchainRegistry,
    private val storageManager: StorageManager,
    private val identifierCreator: IdentifierCreator,
) {
    private val pendingRequests: MutableMap<String, BeaconRequest> = mutableMapOf()

    // -- on incoming --

    public suspend fun onIncomingMessage(origin: Origin, message: VersionedBeaconMessage): Result<BeaconMessage> =
        runCatching {
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

    public suspend fun onOutgoingMessage(
        beaconId: String,
        message: BeaconMessage,
        isTerminal: Boolean,
    ): Result<Pair<Origin, VersionedBeaconMessage>> =
        runCatching {
            when (message) {
                is BeaconResponse -> onOutgoingResponse(message, isTerminal)
                else -> { /* no action */ }
            }

            val senderId = identifierCreator.senderIdentifier(beaconId.asHexString().toByteArray()).getOrThrow()
            Pair(message.associatedOrigin, VersionedBeaconMessage.from(senderId, message))
        }

    private suspend fun onOutgoingResponse(response: BeaconResponse, isTerminal: Boolean) {
        val request = pendingRequests.get(response.id, remove = isTerminal) ?: failWithNoPendingRequest()
        when (response) {
            is PermissionBeaconResponse -> onPermissionResponse(response, request)
            else -> { /* no action */ }
        }
    }

    private suspend fun onPermissionResponse(response: PermissionBeaconResponse, request: BeaconRequest) {
        if (request !is PermissionBeaconRequest) /* unknown state, no action */ return

        val blockchain = blockchainRegistry.get(response.blockchainIdentifier) ?: failWithBlockchainNotFound(response.blockchainIdentifier)
        val permission = blockchain.creator.extractPermission(request, response).getOrThrow()

        storageManager.addPermissions(listOf(permission))
    }

    private fun failWithNoPendingRequest(): Nothing = failWithIllegalArgument("No matching request found")

}