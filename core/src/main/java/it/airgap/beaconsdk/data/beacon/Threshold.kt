package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.Serializable

@Serializable
public data class Threshold(
    public val amount: String,
    public val timeframe: String,
) {
    public companion object {}
}