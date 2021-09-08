package it.airgap.beaconsdk.core.internal.message.v2

import it.airgap.beaconsdk.core.data.beacon.AppMetadata
import kotlinx.serialization.Serializable

@Serializable
internal data class V2AppMetadata(
    val senderId: String,
    val name: String,
    val icon: String? = null,
) {
    fun toAppMetadata(): AppMetadata = AppMetadata(senderId, name, icon)

    companion object {
        fun fromAppMetadata(appMetadata: AppMetadata): V2AppMetadata =
            with(appMetadata) { V2AppMetadata(senderId, name, icon) }
    }
}