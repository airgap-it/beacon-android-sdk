package it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data.api.sync

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
public data class MatrixSyncTimeline(val events: List<MatrixSyncStateEvent<out @Contextual Any>>? = null)