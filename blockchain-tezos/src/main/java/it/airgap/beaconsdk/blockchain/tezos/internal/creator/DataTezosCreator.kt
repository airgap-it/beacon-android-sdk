package it.airgap.beaconsdk.blockchain.tezos.internal.creator

import it.airgap.beaconsdk.blockchain.tezos.data.TezosAppMetadata
import it.airgap.beaconsdk.blockchain.tezos.data.TezosPermission
import it.airgap.beaconsdk.blockchain.tezos.internal.utils.failWithUnknownMessage
import it.airgap.beaconsdk.blockchain.tezos.message.request.PermissionTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.response.PermissionTezosResponse
import it.airgap.beaconsdk.core.data.Account
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.internal.blockchain.creator.DataBlockchainCreator
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.asHexString
import it.airgap.beaconsdk.core.internal.utils.currentTimestamp
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalState
import it.airgap.beaconsdk.core.message.PermissionBeaconRequest
import it.airgap.beaconsdk.core.message.PermissionBeaconResponse
import it.airgap.beaconsdk.core.storage.findAppMetadata

internal class DataTezosCreator(
    private val storageManager: StorageManager,
    private val identifierCreator: IdentifierCreator,
) : DataBlockchainCreator {
    override suspend fun extractIncomingPermission(request: PermissionBeaconRequest, response: PermissionBeaconResponse, origin: Connection.Id): Result<List<Permission>> =
        runCatching {
            if (request !is PermissionTezosRequest) failWithUnknownMessage(request)
            if (response !is PermissionTezosResponse) failWithUnknownMessage(response)

            val peer = storageManager.findPeer { it.publicKey == origin.id } ?: failWithAppMetadataNotFound()
            val senderId = identifierCreator.senderId(origin.id.asHexString().toByteArray()).getOrThrow()
            val appMetadata = TezosAppMetadata(senderId, peer.name, peer.icon)

            listOf(
                TezosPermission(
                    response.account.accountId,
                    senderId,
                    connectedAt = currentTimestamp(),
                    response.account.address,
                    response.account.publicKey,
                    response.account.network,
                    appMetadata,
                    response.scopes,
                ),
            )
        }

    override suspend fun extractOutgoingPermission(request: PermissionBeaconRequest, response: PermissionBeaconResponse): Result<List<Permission>> =
        runCatching {
            if (request !is PermissionTezosRequest) failWithUnknownMessage(request)
            if (response !is PermissionTezosResponse) failWithUnknownMessage(response)

            val appMetadata = storageManager.findAppMetadata<TezosAppMetadata> { it.senderId == request.senderId } ?: failWithAppMetadataNotFound()
            val senderId = identifierCreator.senderId(request.origin.id.asHexString().toByteArray()).getOrThrow()

            listOf(
                TezosPermission(
                    response.account.accountId,
                    senderId,
                    connectedAt = currentTimestamp(),
                    response.account.address,
                    response.account.publicKey,
                    response.account.network,
                    appMetadata,
                    response.scopes,
                ),
            )
        }

    override fun extractAccounts(response: PermissionBeaconResponse): Result<List<Account>> =
        runCatching {
            if (response !is PermissionTezosResponse) failWithUnknownMessage(response)

            listOf(Account(response.account.accountId, response.account.address))
        }

    private fun failWithAppMetadataNotFound(): Nothing = failWithIllegalState("Permission could not be extracted, matching appMetadata not found.")
}