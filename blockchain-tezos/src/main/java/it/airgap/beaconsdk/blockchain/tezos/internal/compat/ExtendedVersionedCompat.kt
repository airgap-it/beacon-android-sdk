package it.airgap.beaconsdk.blockchain.tezos.internal.compat

import it.airgap.beaconsdk.blockchain.tezos.data.TezosPermission
import it.airgap.beaconsdk.core.internal.compat.VersionedCompat
import kotlinx.serialization.KSerializer

internal interface ExtendedVersionedCompat : VersionedCompat {
    val tezosPermissionSerializer: KSerializer<TezosPermission>
}