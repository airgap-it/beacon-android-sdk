package it.airgap.beaconsdk.internal.data

import it.airgap.beaconsdk.data.sdk.Origin

internal data class ConnectionMessage(
    val origin: Origin,
    val content: String
) {
    companion object {}
}