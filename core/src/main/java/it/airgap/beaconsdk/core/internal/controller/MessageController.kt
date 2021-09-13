package it.airgap.beaconsdk.core.internal.controller

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.beacon.Origin
import it.airgap.beaconsdk.core.data.beacon.Permission
import it.airgap.beaconsdk.core.internal.chain.ChainRegistry
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.*
import it.airgap.beaconsdk.core.message.*

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class MessageController internal constructor(
    private val chainRegistry: ChainRegistry,
    private val storageManager: StorageManager,
    private val accountUtils: AccountUtils,
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

            val senderId = accountUtils.getSenderId(beaconId.asHexString().toByteArray()).getOrThrow()
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
        val publicKey = response.publicKey
        // TODO: get type from response/request
        val chain = chainRegistry.get("tezos") ?: failWithChainNotFound("tezos")
        val address = chain.wallet.addressFromPublicKey(publicKey).getOrThrow()
        val accountIdentifier = accountUtils.getAccountIdentifier(address, response.network).getOrThrow()
        val appMetadata = storageManager.findAppMetadata { it.senderId == request.senderId }
            ?: failWithIllegalState("Granted permissions not saved, matching appMetadata could not be found.")

        val permissionInfo = Permission(
            accountIdentifier,
            address,
            response.network,
            response.scopes,
            accountUtils.getSenderId(request.origin.id.asHexString().toByteArray()).getOrThrow(),
            appMetadata,
            publicKey,
            connectedAt = currentTimestamp()
        )

        storageManager.addPermissions(listOf(permissionInfo))
    }

    private fun failWithNoPendingRequest(): Nothing = throw IllegalArgumentException("No matching request found")
}