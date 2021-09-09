package it.airgap.beaconsdk.core.internal.message.v1

import it.airgap.beaconsdk.core.data.beacon.AppMetadata
import kotlinx.serialization.Serializable

@Serializable
public data class V1AppMetadata(
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