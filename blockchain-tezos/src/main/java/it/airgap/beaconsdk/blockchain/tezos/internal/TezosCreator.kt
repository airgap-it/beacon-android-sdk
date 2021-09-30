package it.airgap.beaconsdk.blockchain.tezos.internal

import it.airgap.beaconsdk.blockchain.tezos.data.TezosPermission
import it.airgap.beaconsdk.blockchain.tezos.internal.message.v1.V1TezosMessage
import it.airgap.beaconsdk.blockchain.tezos.internal.message.v2.V2TezosMessage
import it.airgap.beaconsdk.blockchain.tezos.internal.utils.failWithUnknownMessage
import it.airgap.beaconsdk.blockchain.tezos.message.request.PermissionTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.response.PermissionTezosResponse
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.asHexString
import it.airgap.beaconsdk.core.internal.utils.currentTimestamp
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalState
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.PermissionBeaconRequest
import it.airgap.beaconsdk.core.message.PermissionBeaconResponse

internal class TezosCreator(
    private val wallet: TezosWallet,
    private val storageManager: StorageManager,
    private val identifierCreator: IdentifierCreator,
) : Blockchain.Creator {

    // -- data --

    override suspend fun extractPermission(
        request: PermissionBeaconRequest,
        response: PermissionBeaconResponse
    ): Result<Permission> =
        runCatching {
            if (request !is PermissionTezosRequest) failWithUnknownMessage(request)
            if (response !is PermissionTezosResponse) failWithUnknownMessage(response)

            val address = wallet.addressFromPublicKey(response.publicKey).getOrThrow()
            val accountIdentifier = identifierCreator.accountIdentifier(address, response.network).getOrThrow()
            val appMetadata = storageManager.findAppMetadata { it.senderId == request.senderId } ?: failWithAppMetadataNotFound()

            TezosPermission(
                accountIdentifier,
                address,
                identifierCreator.senderIdentifier(request.origin.id.asHexString().toByteArray()).getOrThrow(),
                appMetadata,
                response.publicKey,
                connectedAt = currentTimestamp(),
                response.network,
                response.scopes,
                response.threshold,
            )
        }

    // -- VersionedBeaconMessage --

    override fun v1From(senderId: String, content: BeaconMessage): Result<V1BeaconMessage> = runCatching { V1TezosMessage.from(senderId, content) }
    override fun v2From(senderId: String, content: BeaconMessage): Result<V2BeaconMessage> = runCatching { V2TezosMessage.from(senderId, content) }

    private fun failWithAppMetadataNotFound(): Nothing = failWithIllegalState("Permission could not be extracted, matching appMetadata not found.")
}