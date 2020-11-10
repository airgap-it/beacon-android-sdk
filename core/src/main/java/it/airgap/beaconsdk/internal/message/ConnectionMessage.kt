package it.airgap.beaconsdk.internal.message

import it.airgap.beaconsdk.data.beacon.Origin

internal typealias ConnectionTransportMessage = ConnectionMessage<*>
internal sealed class ConnectionMessage<T> {
    abstract val origin: Origin
    abstract val content: T

    companion object {}
}

internal data class SerializedConnectionMessage(
    override val origin: Origin,
    override val content: String,
) : ConnectionMessage<String>() {
    companion object {}
}

internal data class BeaconConnectionMessage(
    override val origin: Origin,
    override val content: VersionedBeaconMessage,
) : ConnectionMessage<VersionedBeaconMessage>() {
    companion object {}
}