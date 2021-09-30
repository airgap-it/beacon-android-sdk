package it.airgap.beaconsdk.core.internal.compat

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.blockchain.Blockchain

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface VersionedCompat {
    public val withVersion: String
    public val blockchain: Blockchain
}