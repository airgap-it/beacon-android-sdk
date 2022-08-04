package it.airgap.beaconsdk.core.internal.storage.sharedpreferences

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.scope.BeaconScope

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class SharedPreferencesBaseStorage(public val beaconScope: BeaconScope) {

    protected fun Key.scoped(): String =
        when (beaconScope) {
            is BeaconScope.Global -> value
            is BeaconScope.Instance -> "${beaconScope.id}:$value"
        }

    protected interface Key {
        public val value: String
    }
}