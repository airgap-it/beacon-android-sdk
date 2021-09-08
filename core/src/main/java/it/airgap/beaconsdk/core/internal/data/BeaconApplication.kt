package it.airgap.beaconsdk.core.internal.data

import it.airgap.beaconsdk.core.internal.crypto.data.KeyPair

internal data class BeaconApplication(
    val keyPair: KeyPair,
    val name: String,
    val icon: String?,
    val url: String?,
)
