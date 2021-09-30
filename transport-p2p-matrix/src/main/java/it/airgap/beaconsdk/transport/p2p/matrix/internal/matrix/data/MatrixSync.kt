package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data

import it.airgap.beaconsdk.transport.p2p.matrix.data.MatrixRoom
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncResponse

internal data class MatrixSync(
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