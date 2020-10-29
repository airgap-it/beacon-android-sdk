package it.airgap.beaconsdk.internal.message.v1

import it.airgap.beaconsdk.data.beacon.AppMetadata
import kotlinx.serialization.Serializable

@Serializable
internal data class V1AppMetadata(
    val beaconId: String,
    val name: String,
    val icon: String? = null,
) {
    fun toAppMetadata(): AppMetadata = AppMetadata(beaconId, name, icon)

    companion object {
        fun fromAppMetadata(appMetadata: AppMetadata): V1AppMetadata =
            with(appMetadata) { V1AppMetadata(senderId, name, icon) }
    }
}