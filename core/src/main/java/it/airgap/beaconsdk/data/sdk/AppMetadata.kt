package it.airgap.beaconsdk.data.sdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppMetadata(@SerialName("beaconId") val senderId: String, val name: String, val icon: String? = null) {
    companion object {}
}