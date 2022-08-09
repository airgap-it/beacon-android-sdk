package it.airgap.beaconsdk.blockchain.substrate.message.request

import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.message.PermissionBeaconRequest
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateAppMetadata
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateNetwork
import it.airgap.beaconsdk.blockchain.substrate.data.SubstratePermission

public data class PermissionSubstrateRequest internal constructor(
    override val id: String,
    override val version: String,
    override val blockchainIdentifier: String,
    override val senderId: String,
    override val origin: Origin,
    override val destination: Origin?,
    override val appMetadata: SubstrateAppMetadata,
    public val scopes: List<SubstratePermission.Scope>,
    public val networks: List<SubstrateNetwork>,
) : PermissionBeaconRequest() {
    public companion object {}
}