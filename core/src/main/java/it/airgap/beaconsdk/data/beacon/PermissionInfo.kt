package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.Serializable

@Serializable
public data class PermissionInfo(
    public val accountIdentifier: String,
    public val address: String,
    public val network: Network,
    public val scopes: List<PermissionScope>,
    public val senderId: String,
    public val appMetadata: AppMetadata,
    public val website: String,
    public val publicKey: String,
    public val connectedAt: Long,
    public val threshold: Threshold? = null,
) {
    public companion object {}
}