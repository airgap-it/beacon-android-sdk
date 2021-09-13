package it.airgap.beaconsdk.core.internal.message.v2

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.beacon.AppMetadata
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Serializable
public data class V2AppMetadata(
    val senderId: String,
    val name: String,
    val icon: String? = null,
) {
    public fun toAppMetadata(): AppMetadata = AppMetadata(senderId, name, icon)

    public companion object {
        public fun fromAppMetadata(appMetadata: AppMetadata): V2AppMetadata =
            with(appMetadata) { V2AppMetadata(senderId, name, icon) }
    }
}