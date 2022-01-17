package it.airgap.beaconsdk.blockchain.substrate.message.response

import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.message.PermissionBeaconResponse
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateAccount
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateAppMetadata
import it.airgap.beaconsdk.blockchain.substrate.data.SubstratePermission
import it.airgap.beaconsdk.blockchain.substrate.internal.di.extend
import it.airgap.beaconsdk.blockchain.substrate.message.request.PermissionSubstrateRequest
import it.airgap.beaconsdk.core.exception.BeaconException
import it.airgap.beaconsdk.core.internal.utils.dependencyRegistry

public data class PermissionSubstrateResponse internal constructor(
    override val id: String,
    override val version: String,
    override val requestOrigin: Origin,
    override val blockchainIdentifier: String,
    override val accountIds: List<String>,
    public val appMetadata: SubstrateAppMetadata,
    public val scopes: List<SubstratePermission.Scope>,
    public val accounts: List<SubstrateAccount>,
) : PermissionBeaconResponse() {
    public companion object {

        /**
         * Creates a new instance of [PermissionSubstrateResponse] from the [request]
         * with the specified [accounts] and optional [scopes].
         *
         * The response will have an id matching the one of the [request].
         * If no custom [scopes] are provided, the values will be also taken from the [request].
         *
         * @throws [BeaconException] if any of the [accounts] is invalid.
         */
        @Throws(BeaconException::class)
        public fun from(
            request: PermissionSubstrateRequest,
            accounts: List<SubstrateAccount>,
            scopes: List<SubstratePermission.Scope> = request.scopes,
        ): PermissionSubstrateResponse {
            val accountsIds = accounts.map {
                val address = dependencyRegistry.extend().substrateWallet.address(it.publicKey, it.addressPrefix).getOrThrow()
                dependencyRegistry.identifierCreator.accountId(address, it.network).getOrThrow()
            }

            return PermissionSubstrateResponse(
                request.id,
                request.version,
                request.origin,
                request.blockchainIdentifier,
                accountsIds,
                request.appMetadata,
                scopes,
                accounts,
            )
        }
    }
}
