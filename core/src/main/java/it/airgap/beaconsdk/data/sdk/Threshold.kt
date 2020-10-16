package it.airgap.beaconsdk.data.sdk

import kotlinx.serialization.Serializable

@Serializable
data class Threshold(val amount: String, val timeframe: String) {
    companion object {}
}