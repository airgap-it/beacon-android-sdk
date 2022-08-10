package it.airgap.beaconsdk.core.internal.controller

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.compat.Compat
import it.airgap.beaconsdk.core.internal.compat.VersionedCompat
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.asHexString
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalArgument
import it.airgap.beaconsdk.core.internal.utils.get
import it.airgap.beaconsdk.core.message.*
import it.airgap.beaconsdk.core.scope.BeaconScope

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MessageController internal constructor(
    private val beaconScope: BeaconScope,
    private val blockchainRegistry: BlockchainRegistry,
    private val storageManager: StorageManager,
    private val identifierCreator: IdentifierCreator,
    private val compat: Compat<VersionedCompat>,
) {
    private val outgoingRequests: MutableMap<String, BeaconRequest> = mutableMapOf()
    private val incomingRequests: MutableMap<String, BeaconRequest> = mutableMapOf()

    // -- on incoming --

    public suspend fun onIncomingMessage(origin: Origin, destination: Origin, message: VersionedBeaconMessage): Result<Pair<Origin, BeaconMessage>> =
        runCatching {
            val message = message.toBeaconMessage(origin, destination, beaconScope).also {
                when (it) {
                    is BeaconRequest -> onIncomingRequest(it)
                    is BeaconResponse -> onIncomingResponse(it)
                    else -> { /* no action */ }
                }
            }

            Pair(origin, message)
        }

    private suspend fun onIncomingRequest(request: BeaconRequest) {
        incomingRequests[request.id] = request

        when (request) {
            is PermissionBeaconRequest -> onIncomingPermissionRequest(request)
            else -> { /* no action */ }
        }
    }

    private suspend fun onIncomingPermissionRequest(request: PermissionBeaconRequest) {
        storageManager.addAppMetadata(listOf(request.appMetadata))
    }

    private suspend fun onIncomingResponse(response: BeaconResponse) {
        val request = outgoingRequests.get(response.id, remove = response.isTerminal) ?: failWithNoPendingRequest()
        when (response) {
            is PermissionBeaconResponse -> onIncomingPermissionResponse(response, request)
            else -> { /* no action */ }
        }
    }

    private suspend fun onIncomingPermissionResponse(response: PermissionBeaconResponse, request: BeaconRequest) {
        if (request !is PermissionBeaconRequest) /* unknown state, no action */ return

        val blockchain = blockchainRegistry.get(response.blockchainIdentifier)
        val permissions = blockchain.creator.data.extractIncomingPermission(request, response).getOrThrow()

        storageManager.addPermissions(permissions) // TODO: replace if accountId & senderId are the same
    }

    private val BeaconResponse.isTerminal: Boolean
        get() = when (this) {
            is AcknowledgeBeaconResponse -> false
            else -> true
        }

    // -- on outgoing --

    public suspend fun onOutgoingMessage(
        beaconId: String,
        message: BeaconMessage,
        isTerminal: Boolean,
    ): Result<Pair<Origin?, VersionedBeaconMessage>> =
        runCatching {
            when (message) {
                is BeaconRequest -> onOutgoingRequest(message, isTerminal)
                is BeaconResponse -> onOutgoingResponse(message, isTerminal)
                else -> { /* no action */ }
            }

            val senderId = identifierCreator.senderId(beaconId.asHexString().toByteArray()).getOrThrow()
            Pair(
                message.destination,
                VersionedBeaconMessage.from(
                    senderId,
                    message,
                    VersionedBeaconMessage.Context(blockchainRegistry, compat)
                ),
            )
        }

    private suspend fun onOutgoingRequest(request: BeaconRequest, isTerminal: Boolean) {
        if (!isTerminal) outgoingRequests[request.id] = request
    }

    private suspend fun onOutgoingResponse(response: BeaconResponse, isTerminal: Boolean) {
        val request = incomingRequests.get(response.id, remove = isTerminal) ?: failWithNoPendingRequest()
        when (response) {
            is PermissionBeaconResponse -> onOutgoingPermissionResponse(response, request)
            else -> { /* no action */ }
        }
    }

    private suspend fun onOutgoingPermissionResponse(response: PermissionBeaconResponse, request: BeaconRequest) {
        if (request !is PermissionBeaconRequest) /* unknown state, no action */ return

        val blockchain = blockchainRegistry.get(response.blockchainIdentifier)
        val permissions = blockchain.creator.data.extractOutgoingPermission(request, response).getOrThrow()

        storageManager.addPermissions(permissions) // TODO: replace if accountId & senderId are the same
    }

    private fun failWithNoPendingRequest(): Nothing = failWithIllegalArgument("No matching request found")

}