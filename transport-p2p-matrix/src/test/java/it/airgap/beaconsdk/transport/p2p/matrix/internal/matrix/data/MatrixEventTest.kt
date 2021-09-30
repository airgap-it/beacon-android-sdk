package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data

import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncRoom
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncRooms
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncState
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncStateEvent.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class MatrixEventTest {

    @Test
    fun `creates list of events from sync rooms response`() {
        val node = "node"
        val roomId = "roomId"
        val sender = "sender"

        val syncRooms = MatrixSyncRooms(
            join = mapOf(
                roomId to MatrixSyncRoom.Joined(
                    MatrixSyncState(
                        listOf(
                            Member(Member.Content(Member.Membership.Invite), sender = sender),
                            Member(Member.Content(Member.Membership.Join), sender = sender),
                            Member(Member.Content(Member.Membership.Ban)),
                        )
                    )
                ),
            ),
        )

        val events = MatrixEvent.fromSync(node, syncRooms)

        assertEquals(
            listOf(MatrixEvent.Invite(node, roomId, sender), MatrixEvent.Join(node, roomId, sender)),
            events,
        )
    }

    @Test
    fun `creates list of events from sync room`() {
        val node = "node"
        val roomId = "roomId"
        val sender = "sender"

        val syncRoom = MatrixSyncRoom.Joined(
            MatrixSyncState(
                listOf(
                    Member(Member.Content(Member.Membership.Invite), sender = sender),
                    Member(Member.Content(Member.Membership.Join), sender = sender),
                    Member(Member.Content(Member.Membership.Ban)),
                )
            )
        )

        val events = MatrixEvent.fromSync(node, roomId, syncRoom)

        assertEquals(
            listOf(MatrixEvent.Invite(node, roomId, sender), MatrixEvent.Join(node, roomId, sender)),
            events,
        )
    }

    @Test
    fun `creates list of events from list of sync events`() {
        val node = "node"
        val roomId = "roomId"
        val sender = "sender"

        val syncEvents = listOf(
            Member(Member.Content(Member.Membership.Invite), sender = sender),
            Member(Member.Content(Member.Membership.Join), sender = sender),
            Member(Member.Content(Member.Membership.Ban)),
        )

        val events = MatrixEvent.fromSync(node, roomId, syncEvents)

        assertEquals(
            listOf(MatrixEvent.Invite(node, roomId, sender), MatrixEvent.Join(node, roomId, sender)),
            events,
        )
    }

    @Test
    fun `creates event from sync event`() {
        val node = "node"
        val roomId = "roomId"
        val sender = "sender"
        val message = "message"

        val syncCreate = Create(Create.Content(sender), sender = sender)
        val create = MatrixEvent.fromSync(node, roomId, syncCreate)

        assertEquals(MatrixEvent.Create(node, roomId, sender), create)

        val syncInvite = Member(Member.Content(Member.Membership.Invite), sender = sender)
        val invite = MatrixEvent.fromSync(node, roomId, syncInvite)

        assertEquals(MatrixEvent.Invite(node, roomId, sender), invite)

        val syncJoin = Member(Member.Content(Member.Membership.Join), sender = sender)
        val join = MatrixEvent.fromSync(node, roomId, syncJoin)

        assertEquals(MatrixEvent.Join(node, roomId, sender), join)

        val syncTextMessage = Message(Message.Content(Message.Content.TYPE_TEXT, message), sender = sender)
        val textMessage = MatrixEvent.fromSync(node, roomId, syncTextMessage)

        assertEquals(MatrixEvent.TextMessage(node, roomId, sender, message), textMessage)

        val syncUnknown = Member(Member.Content(Member.Membership.Ban))
        val unknown = MatrixEvent.fromSync(node, roomId, syncUnknown)

        assertNull(unknown, "Expected unknown sync event to be mapped to null")
    }
}