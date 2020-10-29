package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.Serializable

@Serializable
public data class AppMetadata(
    public val senderId: String,
    public val name: String,
    public val icon: String? = null,
) {
    public companion object {}
}