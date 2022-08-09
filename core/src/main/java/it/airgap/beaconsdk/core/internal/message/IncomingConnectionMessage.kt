package it.airgap.beaconsdk.core.internal.message

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.Origin

// -- incoming --

internal typealias IncomingConnectionTransportMessage = IncomingConnectionMessage<*>

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed interface IncomingConnectionMessage<T> {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val origin: Origin

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val content: T

    public companion object {}
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class SerializedIncomingConnectionMessage(
    override val origin: Origin,
    override val content: String,
) : IncomingConnectionMessage<String> {
    public constructor(pair: Pair<Origin, String>) : this(pair.first, pair.second)

    public companion object {}
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class BeaconIncomingConnectionMessage(
    override val origin: Origin,
    override val content: VersionedBeaconMessage,
) : IncomingConnectionMessage<VersionedBeaconMessage> {
    public constructor(pair: Pair<Origin, VersionedBeaconMessage>) : this(pair.first, pair.second)

    public companion object {}
}

// -- outgoing --

internal typealias OutgoingConnectionTransportMessage = OutgoingConnectionMessage<*>

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed interface OutgoingConnectionMessage<T> {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val destination: Origin?

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val content: T

    public companion object {}
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class SerializedOutgoingConnectionMessage(
    override val destination: Origin?,
    override val content: String,
) : OutgoingConnectionMessage<String> {
    public constructor(pair: Pair<Origin?, String>) : this(pair.first, pair.second)

    public companion object {}
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class BeaconOutgoingConnectionMessage(
    override val destination: Origin?,
    override val content: VersionedBeaconMessage,
) : OutgoingConnectionMessage<VersionedBeaconMessage> {
    public constructor(pair: Pair<Origin?, VersionedBeaconMessage>) : this(pair.first, pair.second)

    public companion object {}
}