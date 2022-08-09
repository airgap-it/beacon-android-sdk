package it.airgap.beaconsdk.core.scope

import androidx.annotation.RestrictTo

public sealed interface BeaconScope {
    public object Global : BeaconScope
    public data class Instance(public val id: String) : BeaconScope
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Scope(id: String?, prefix: String? = null): BeaconScope {
    val prefix = prefix?.let { "__${prefix.trim('_')}__" }

    return when {
        id != null && prefix != null -> BeaconScope.Instance("$prefix${id.trimStart('_')}")
        id != null -> BeaconScope.Instance(id)
        prefix != null -> BeaconScope.Instance(prefix)
        else -> BeaconScope.Global
    }
}