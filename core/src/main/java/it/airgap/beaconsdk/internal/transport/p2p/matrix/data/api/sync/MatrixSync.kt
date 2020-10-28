package it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MatrixSyncResponse(
    @SerialName("next_batch") val nextBatch: String? = null,
    val rooms: Rooms? = null,
) {

    @Serializable
    data class Rooms(
        val join: Map<String, MatrixSyncRoom.Joined>? = null,
        val invite: Map<String, MatrixSyncRoom.Invited>? = null,
        val leave: Map<String, MatrixSyncRoom.Left>? = null,
    )
}