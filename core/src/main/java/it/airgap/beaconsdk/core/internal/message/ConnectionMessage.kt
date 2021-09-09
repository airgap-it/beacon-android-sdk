package it.airgap.beaconsdk.core.internal.message

import it.airgap.beaconsdk.core.data.beacon.Origin

public typealias ConnectionTransportMessage = ConnectionMessage<*>
public sealed interface ConnectionMessage<T> {
    val origin: Origin
    val content: T

    companion object {}
}

public data class SerializedConnectionMessage(
    override val origin: Origin,
    override val content: String,
) : ConnectionMessage<String> {
    constructor(pair: Pair<Origin, String>) : this(pair.first, pair.second)

    companion object {}
}

public data class BeaconConnectionMessage(
    override val origin: Origin,
    override val content: VersionedBeaconMessage,
) : ConnectionMessage<VersionedBeaconMessage> {
    constructor(pair: Pair<Origin, VersionedBeaconMessage>) : this(pair.first, pair.second)

    companion object {}
}