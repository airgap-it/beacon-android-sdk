package it.airgap.beaconsdk.core.internal.data

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.crypto.data.KeyPair

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class BeaconApplication(
    val keyPair: KeyPair,
    val name: String,
    val icon: String?,
    val url: String?,
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public data class Partial(
        val name: String,
        val icon: String? = null,
        val url: String? = null,
    ) {
        internal fun toFinal(keyPair: KeyPair): BeaconApplication = BeaconApplication(keyPair, name, icon, url)
    }
}
