package it.airgap.beaconsdk.data.account

import it.airgap.beaconsdk.data.network.Network
import it.airgap.beaconsdk.data.sdk.Origin
import it.airgap.beaconsdk.data.sdk.Threshold
import it.airgap.beaconsdk.data.permission.PermissionScope
import kotlinx.serialization.Serializable

@Serializable
data class AccountInfo(
    val accountIdentifier: String,
    val address: String,
    val network: Network,
    val scopes: List<PermissionScope>,
    val senderId: String,
    val origin: Origin,
    val publicKey: String,
    val connectedAt: Long,
    val threshold: Threshold? = null
) {
    companion object {}
}