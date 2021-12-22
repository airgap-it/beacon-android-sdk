package it.airgap.blockchain.substrate.message.response

import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.message.PermissionBeaconResponse
import it.airgap.blockchain.substrate.data.SubstrateAccount
import it.airgap.blockchain.substrate.data.SubstrateAppMetadata
import it.airgap.blockchain.substrate.data.SubstratePermission

public data class PermissionSubstrateResponse(
    override val id: String,
    override val version: String,
    override val requestOrigin: Origin,
    override val blockchainIdentifier: String,
    override val accountId: String,
    public val appMetadata: SubstrateAppMetadata,
    public val scopes: List<SubstratePermission.Scope>,
    public val accounts: List<SubstrateAccount>,
) : PermissionBeaconResponse() {
    public companion object {}
}
