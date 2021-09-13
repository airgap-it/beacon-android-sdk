package it.airgap.beaconsdk.core.internal.data

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.crypto.data.KeyPair

@RestrictTo(RestrictTo.Scope.LIBRARY)
public data class BeaconApplication(
    val keyPair: KeyPair,
    val name: String,
    val icon: String?,
    val url: String?,
)
