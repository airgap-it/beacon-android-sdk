package it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data

import it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data.api.sync.MatrixSyncResponse

public data class MatrixSync(
    val nextBatch: String? = null,
    val rooms: List<MatrixRoom>? = null,
    val events: List<MatrixEvent>? = null,
) {
    companion object {
        fun fromSyncResponse(node: String, response: MatrixSyncResponse): MatrixSync =
            with(response) {
                val roomList = rooms?.let { MatrixRoom.fromSync(node, rooms) }
                val eventList = rooms?.let { MatrixEvent.fromSync(node, rooms) }

                return MatrixSync(nextBatch, roomList, eventList)
            }
    }
}