package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
internal data class MatrixSyncTimeline(val events: List<MatrixSyncStateEvent<out @Contextual Any>>? = null)