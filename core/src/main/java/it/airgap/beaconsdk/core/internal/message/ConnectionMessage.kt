package it.airgap.beaconsdk.core.internal.message

import it.airgap.beaconsdk.core.data.beacon.Origin

internal typealias ConnectionTransportMessage = ConnectionMessage<*>
internal sealed interface ConnectionMessage<T> {
    val origin: Origin
    val content: T

    companion object {}
}

internal data class SerializedConnectionMessage(
    override val origin: Origin,
    override val content: String,
) : ConnectionMessage<String> {
    constructor(pair: Pair<Origin, String>) : this(pair.first, pair.second)

    companion object {}
}

internal data class BeaconConnectionMessage(
    override val origin: Origin,
    override val content: VersionedBeaconMessage,
) : ConnectionMessage<VersionedBeaconMessage> {
    constructor(pair: Pair<Origin, VersionedBeaconMessage>) : this(pair.first, pair.second)

    companion object {}
}