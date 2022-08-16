package it.airgap.beaconsdk.core.internal.compat.v2_0_0

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.internal.compat.VersionedCompat
import it.airgap.beaconsdk.core.internal.utils.blockchainRegistry
import it.airgap.beaconsdk.core.scope.BeaconScope

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("ClassName")
public class CompatWithV2_0_0(private val beaconScope: BeaconScope? = null) : VersionedCompat {
    override val withVersion: String = "2.0.0"

    override val blockchain: Blockchain
        get() = blockchainRegistry(beaconScope).get(CHAIN_IDENTIFIER)

    public companion object {
        private const val CHAIN_IDENTIFIER: String = "tezos"
    }
}