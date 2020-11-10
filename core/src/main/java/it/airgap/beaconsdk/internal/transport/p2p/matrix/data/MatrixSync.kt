package it.airgap.beaconsdk.internal.transport.p2p.matrix.data

import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncResponse

internal data class MatrixSync(
    val nextBatch: String? = null,
    val rooms: List<MatrixRoom>? = null,
    val events: List<MatrixEvent>? = null,
) {
    companion object {
        fun fromSyncResponse(response: MatrixSyncResponse): MatrixSync =
            with(response) {
                val roomList = rooms?.let { MatrixRoom.fromSync(rooms) }
                val eventList = rooms?.let { MatrixEvent.fromSync(rooms) }

                return MatrixSync(nextBatch, roomList, eventList)
            }
    }
}