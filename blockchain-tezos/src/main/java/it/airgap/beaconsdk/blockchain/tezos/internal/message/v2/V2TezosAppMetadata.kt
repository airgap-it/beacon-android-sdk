package it.airgap.beaconsdk.blockchain.tezos.internal.message.v2

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.blockchain.tezos.data.TezosAppMetadata
import it.airgap.beaconsdk.core.data.AppMetadata
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
public data class V2TezosAppMetadata(
    val senderId: String,
    val name: String,
    val icon: String? = null,
) {
    public fun toAppMetadata(): TezosAppMetadata = TezosAppMetadata(senderId, name, icon)

    public companion object {
        public fun fromAppMetadata(appMetadata: TezosAppMetadata): V2TezosAppMetadata =
            with(appMetadata) { V2TezosAppMetadata(senderId, name, icon) }
    }
}