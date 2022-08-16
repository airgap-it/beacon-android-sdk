package it.airgap.beaconsdk.core.internal.compat

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.compat.v2_0_0.CompatWithV2_0_0
import it.airgap.beaconsdk.core.scope.BeaconScope

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CoreCompat(beaconScope: BeaconScope? = null) : Compat<VersionedCompat>() {
    init {
        register(CompatWithV2_0_0(beaconScope))
    }
}