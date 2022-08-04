package it.airgap.beaconsdk.core.scope

import androidx.annotation.RestrictTo

public sealed interface BeaconScope {
    public object Global : BeaconScope
    public data class Instance(public val id: String) : BeaconScope
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Scope(id: String?, prefix: String? = null): BeaconScope =
    when {
        id != null && prefix != null -> BeaconScope.Instance("__${prefix.trim('_')}__${id.trimStart('_')}")
        id != null -> BeaconScope.Instance(id)
        else -> BeaconScope.Global
    }