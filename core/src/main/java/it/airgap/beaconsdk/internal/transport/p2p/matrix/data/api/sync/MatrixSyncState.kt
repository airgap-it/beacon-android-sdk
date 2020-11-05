package it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
internal data class MatrixSyncState(val events: List<MatrixSyncStateEvent<out @Contextual Any>>? = null)