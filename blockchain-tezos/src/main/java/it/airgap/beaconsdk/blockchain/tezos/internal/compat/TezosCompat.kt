package it.airgap.beaconsdk.blockchain.tezos.internal.compat

import it.airgap.beaconsdk.blockchain.tezos.internal.compat.v2_0_0.CompatWithV2_0_0
import it.airgap.beaconsdk.core.internal.compat.Compat

internal object TezosCompat : Compat<ExtendedVersionedCompat>() {
    init {
        register(CompatWithV2_0_0)
    }
}