package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MatrixSyncResponse(
    @SerialName("next_batch") val nextBatch: String? = null,
    val rooms: MatrixSyncRooms? = null,
)