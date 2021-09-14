package it.airgap.beaconsdk.core.internal.message.v1

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.AppMetadata
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Serializable
public data class V1AppMetadata(
    val beaconId: String,
    val name: String,
    val icon: String? = null,
) {
    public fun toAppMetadata(): AppMetadata = AppMetadata(beaconId, name, icon)

    public companion object {
        public fun fromAppMetadata(appMetadata: AppMetadata): V1AppMetadata =
            with(appMetadata) { V1AppMetadata(senderId, name, icon) }
    }
}