package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.Serializable

@Serializable
public data class AccountInfo(
    public val accountIdentifier: String,
    public val address: String,
    public val network: Network,
    public val scopes: List<PermissionScope>,
    public val senderId: String,
    public val origin: Origin,
    public val publicKey: String,
    public val connectedAt: Long,
    public val threshold: Threshold? = null,
) {
    public companion object {}
}