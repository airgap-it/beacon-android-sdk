package it.airgap.beaconsdk.core.scope

import androidx.annotation.RestrictTo

public sealed interface BeaconScope {
    public object Global : BeaconScope
    public data class Instance(public val id: String) : BeaconScope
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun BeaconScope(id: String?): BeaconScope =
    id?.let { BeaconScope.Instance(it) } ?: BeaconScope.Global