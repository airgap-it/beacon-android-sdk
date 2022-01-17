package it.airgap.beaconsdk.core.internal.compat.v2_0_0

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.internal.compat.VersionedCompat
import it.airgap.beaconsdk.core.internal.utils.blockchainRegistry

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("ClassName")
public object CompatWithV2_0_0 : VersionedCompat {
    override val withVersion: String = "2.0.0"

    private const val CHAIN_IDENTIFIER: String = "tezos"
    override val blockchain: Blockchain
        get() = blockchainRegistry.get(CHAIN_IDENTIFIER)
}