package it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MatrixSyncResponse(
    @SerialName("next_batch") val nextBatch: String?,
    val rooms: Rooms?,
) {

    @Serializable
    data class Rooms(
        val join: Map<String, MatrixSyncRoom.Joined>,
        val invite: Map<String, MatrixSyncRoom.Invited>,
        val left: Map<String, MatrixSyncRoom.Left>,
    )
}