package it.airgap.beaconsdk.internal.message

import it.airgap.beaconsdk.data.beacon.Origin

internal data class SerializedBeaconMessage(val origin: Origin, val content: String) {
    companion object {}
}