package it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data.api.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class MatrixSyncResponse(
    @SerialName("next_batch") val nextBatch: String? = null,
    val rooms: MatrixSyncRooms? = null,
)