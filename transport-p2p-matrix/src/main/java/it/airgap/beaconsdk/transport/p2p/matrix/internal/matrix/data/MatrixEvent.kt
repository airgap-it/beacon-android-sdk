package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data

import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncRoom
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncRooms
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncStateEvent

internal sealed interface MatrixEvent {
    val node: String

    data class Create(override val node: String, val roomId: String, val creator: String) : MatrixEvent

    data class Invite(override val node: String, val roomId: String, val sender: String) : MatrixEvent
    data class Join(override val node: String, val roomId: String, val userId: String) : MatrixEvent

    data class TextMessage(override val node: String, val roomId: String, val sender: String, val message: String) : MatrixEvent

    companion object {
        fun fromSync(node: String, syncRooms: MatrixSyncRooms): List<MatrixEvent> {
            val joinEvents =
                syncRooms.join?.entries?.flatMap { fromSync(node, it.key, it.value) } ?: emptyList()

            val inviteEvents =
                syncRooms.invite?.entries?.flatMap { fromSync(node, it.key, it.value) } ?: emptyList()

            val leftEvents =
                syncRooms.leave?.entries?.flatMap { fromSync(node, it.key, it.value) } ?: emptyList()

            return joinEvents + inviteEvents + leftEvents
        }

        fun fromSync(node: String, id: String, syncRoom: MatrixSyncRoom): List<MatrixEvent> =
            with(syncRoom) {
                when (this) {
                    is MatrixSyncRoom.Joined ->
                        fromSync(node, id, state?.events ?: emptyList()) + fromSync(node, id, timeline?.events ?: emptyList())

                    is MatrixSyncRoom.Invited ->
                        fromSync(node, id, state?.events ?: emptyList())

                    is MatrixSyncRoom.Left ->
                        fromSync(node, id, state?.events ?: emptyList()) + fromSync(node, id, timeline?.events ?: emptyList())
                }
            }

        fun fromSync(node: String, roomId: String, syncEvents: List<MatrixSyncStateEvent<*>>): List<MatrixEvent> =
            syncEvents.mapNotNull { fromSync(node, roomId, it) }

        fun fromSync(node: String, roomId: String, syncEvent: MatrixSyncStateEvent<*>): MatrixEvent? =
            with(syncEvent) {
                return when (this) {
                    is MatrixSyncStateEvent.Create -> content?.creator?.let { Create(node, roomId, it) }
                    is MatrixSyncStateEvent.Member -> {
                        when (content?.membership) {
                            MatrixSyncStateEvent.Member.Membership.Invite -> sender?.let { Invite(node, roomId, it) }
                            MatrixSyncStateEvent.Member.Membership.Join -> sender?.let { Join(node, roomId, it) }
                            else -> null
                        }
                    }
                    is MatrixSyncStateEvent.Message -> {
                        val sender = sender ?: return null
                        val type = content?.messageType ?: return null
                        val body = content.body ?: return null

                        when (type) {
                            MatrixSyncStateEvent.Message.Content.TYPE_TEXT -> TextMessage(node, roomId, sender, body)
                            else -> null
                        }
                    }
                    else -> null
                }
            }
    }
}