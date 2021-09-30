package it.airgap.beaconsdk.blockchain.tezos.internal.compat.v2_0_0

import it.airgap.beaconsdk.blockchain.tezos.data.TezosPermission
import it.airgap.beaconsdk.blockchain.tezos.internal.compat.ExtendedVersionedCompat
import it.airgap.beaconsdk.core.internal.compat.VersionedCompat
import kotlinx.serialization.KSerializer
import it.airgap.beaconsdk.core.internal.compat.v2_0_0.CompatWithV2_0_0 as CoreCompatWithV2_0_0

@Suppress("ClassName")
internal object CompatWithV2_0_0 : ExtendedVersionedCompat, VersionedCompat by CoreCompatWithV2_0_0 {
    override val tezosPermissionSerializer: KSerializer<TezosPermission> get() = TezosPermissionSerializer
}