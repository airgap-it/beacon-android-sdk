package it.airgap.beaconsdk.blockchain.tezos.internal.message.v1

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.blockchain.tezos.data.TezosAppMetadata
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
public data class V1TezosAppMetadata(
    val beaconId: String,
    val name: String,
    val icon: String? = null,
) {
    public fun toAppMetadata(): TezosAppMetadata = TezosAppMetadata(beaconId, name, icon)

    public companion object {

        public fun fromAppMetadata(appMetadata: TezosAppMetadata): V1TezosAppMetadata =
            with(appMetadata) { V1TezosAppMetadata(senderId, name, icon) }
    }
}