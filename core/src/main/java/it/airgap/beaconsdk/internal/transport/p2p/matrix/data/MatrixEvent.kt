package it.airgap.beaconsdk.internal.transport.p2p.matrix.data

import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncRoom
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncRooms
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncStateEvent

internal sealed class MatrixEvent {

    data class Create(val creator: String) : MatrixEvent()

    data class Invite(val roomId: String) : MatrixEvent()
    data class Join(val roomId: String, val userId: String) : MatrixEvent()

    data class TextMessage(val roomId: String, val sender: String, val message: String) : MatrixEvent()

    companion object {
        fun fromSync(syncRooms: MatrixSyncRooms): List<MatrixEvent?> {
            val joinEvents =
                syncRooms.join?.entries?.flatMap { fromSync(it.key, it.value) } ?: emptyList()

            val inviteEvents =
                syncRooms.invite?.entries?.flatMap { fromSync(it.key, it.value) } ?: emptyList()

            val leftEvents =
                syncRooms.leave?.entries?.flatMap { fromSync(it.key, it.value) } ?: emptyList()

            return joinEvents + inviteEvents + leftEvents
        }

        fun fromSync(id: String, syncRoom: MatrixSyncRoom): List<MatrixEvent?> =
            with(syncRoom) {
                when (this) {
                    is MatrixSyncRoom.Joined ->
                        fromSync(id, state?.events ?: emptyList()) + fromSync(id, timeline?.events ?: emptyList())

                    is MatrixSyncRoom.Invited ->
                        fromSync(id, state?.events ?: emptyList())

                    is MatrixSyncRoom.Left ->
                        fromSync(id, state?.events ?: emptyList()) + fromSync(id, timeline?.events ?: emptyList())
                }
            }

        fun fromSync(roomId: String, syncEvents: List<MatrixSyncStateEvent<*>>): List<MatrixEvent?> =
            syncEvents.map { fromSync(roomId, it) }

        fun fromSync(roomId: String, syncEvent: MatrixSyncStateEvent<*>): MatrixEvent? =
            with(syncEvent) {
                return when (this) {
                    is MatrixSyncStateEvent.Create -> content?.creator?.let { Create(it) }
                    is MatrixSyncStateEvent.Member -> {
                        when (content?.membership) {
                            MatrixSyncStateEvent.Member.Membership.Invite -> Invite(roomId)
                            MatrixSyncStateEvent.Member.Membership.Join -> sender?.let { Join(roomId, it) }
                            else -> null
                        }
                    }
                    is MatrixSyncStateEvent.Message -> {
                        val sender = sender ?: return null
                        val body = content?.body ?: return null
                        TextMessage(roomId, sender, body)
                    }
                    else -> null
                }
            }
    }
}