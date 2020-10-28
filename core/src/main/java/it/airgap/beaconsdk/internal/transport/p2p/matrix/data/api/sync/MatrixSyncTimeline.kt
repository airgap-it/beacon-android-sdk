package it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync

import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.event.MatrixStateEvent
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
internal data class MatrixSyncTimeline(val events: List<MatrixStateEvent<@Contextual Any>>? = null)