package it.airgap.beaconsdk.core.internal.compat

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.compat.v2_0_0.CompatWithV2_0_0

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object CoreCompat : Compat<VersionedCompat>() {
    init {
        register(CompatWithV2_0_0)
    }
}