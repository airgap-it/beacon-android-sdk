package it.airgap.beaconsdk.data.permission

import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.data.network.Network
import it.airgap.beaconsdk.data.sdk.Threshold
import kotlinx.serialization.Serializable

@Serializable
data class PermissionInfo(
    val accountIdentifier: String,
    val address: String,
    val network: Network,
    val scopes: List<PermissionScope>,
    val senderId: String,
    val appMetadata: AppMetadata,
    val website: String,
    val publicKey: String,
    val connectedAt: Long,
    val threshold: Threshold? = null
) {
    companion object {}
}