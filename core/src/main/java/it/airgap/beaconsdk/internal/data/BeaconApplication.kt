package it.airgap.beaconsdk.internal.data

import it.airgap.beaconsdk.internal.crypto.data.KeyPair

internal data class BeaconApplication(
    val keyPair: KeyPair,
    val name: String,
    val icon: String?,
    val url: String?,
)
