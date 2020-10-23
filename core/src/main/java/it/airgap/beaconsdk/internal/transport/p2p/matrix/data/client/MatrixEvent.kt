package it.airgap.beaconsdk.internal.transport.p2p.matrix.data.client

import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.event.MatrixStateEvent
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncResponse
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncRoom

internal sealed class MatrixEvent {

    data class Create(val creator: String) : MatrixEvent()

    data class Invite(val roomId: String) : MatrixEvent()
    data class Join(val roomId: String, val userId: String) : MatrixEvent()

    data class TextMessage(val roomId: String, val sender: String, val message: String) : MatrixEvent()

    companion object {
        fun fromSync(syncRooms: MatrixSyncResponse.Rooms): List<MatrixEvent?> {
            val joinEvents = syncRooms.join.entries.flatMap { fromSync(it.key, it.value) }
            val inviteEvents = syncRooms.invite.entries.flatMap { fromSync(it.key, it.value) }
            val leftEvents = syncRooms.left.entries.flatMap { fromSync(it.key, it.value) }
            
            return joinEvents + inviteEvents + leftEvents
        }

        fun fromSync(id: String, syncRoom: MatrixSyncRoom): List<MatrixEvent?> =
            with(syncRoom) {
                when (this) {
                    is MatrixSyncRoom.Joined -> {
                        fromSync(id, state?.events ?: emptyList()) + fromSync(id, timeline?.events ?: emptyList())
                    }
                    is MatrixSyncRoom.Invited -> {
                        fromSync(id, state?.events ?: emptyList())
                    }
                    is MatrixSyncRoom.Left -> {
                        fromSync(id, state?.events ?: emptyList()) + fromSync(id, timeline?.events ?: emptyList())
                    }
                }
            }

        fun fromSync(roomId: String, syncEvents: List<MatrixStateEvent<*>>): List<MatrixEvent?> =
            syncEvents.map { fromSync(roomId, it) }

        fun fromSync(roomId: String, syncEvent: MatrixStateEvent<*>): MatrixEvent? =
            with (syncEvent) {
                return when (this) {
                    is MatrixStateEvent.Create -> content?.creator?.let { Create(it) }
                    is MatrixStateEvent.Member -> {
                        when (content?.membership) {
                            MatrixStateEvent.Member.Membership.Invite -> Invite(roomId)
                            MatrixStateEvent.Member.Membership.Join -> sender?.let { Join(roomId, it) }
                            else -> null
                        }
                    }
                    is MatrixStateEvent.Message -> {
                        val sender = sender ?: return null
                        val body = content?.body ?: return null
                        TextMessage(roomId, sender, body)
                    }
                    else -> null
                }
            }
    }
}