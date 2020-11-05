package it.airgap.beaconsdk.internal.transport.p2p.matrix.data

import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncRoom
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncRooms
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncState
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncStateEvent.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class MatrixEventTest {

    @Test
    fun `creates list of events from sync rooms response`() {
        val roomId = "roomId"
        val sender = "sender"

        val syncRooms = MatrixSyncRooms(
            join = mapOf(
                roomId to MatrixSyncRoom.Joined(
                    MatrixSyncState(
                        listOf(
                            Member(Member.Content(Member.Membership.Invite)),
                            Member(Member.Content(Member.Membership.Join), sender = sender),
                            Member(Member.Content(Member.Membership.Ban)),
                        )
                    )
                ),
            ),
        )

        val events = MatrixEvent.fromSync(syncRooms)

        assertEquals(
            listOf(MatrixEvent.Invite(roomId), MatrixEvent.Join(roomId, sender)),
            events,
        )
    }

    @Test
    fun `creates list of events from sync room`() {
        val roomId = "roomId"
        val sender = "sender"

        val syncRoom = MatrixSyncRoom.Joined(
            MatrixSyncState(
                listOf(
                    Member(Member.Content(Member.Membership.Invite)),
                    Member(Member.Content(Member.Membership.Join), sender = sender),
                    Member(Member.Content(Member.Membership.Ban)),
                )
            )
        )

        val events = MatrixEvent.fromSync(roomId, syncRoom)

        assertEquals(
            listOf(MatrixEvent.Invite(roomId), MatrixEvent.Join(roomId, sender)),
            events,
        )
    }

    @Test
    fun `creates list of events from list of sync events`() {
        val roomId = "roomId"
        val sender = "sender"

        val syncEvents = listOf(
            Member(Member.Content(Member.Membership.Invite)),
            Member(Member.Content(Member.Membership.Join), sender = sender),
            Member(Member.Content(Member.Membership.Ban)),
        )

        val events = MatrixEvent.fromSync(roomId, syncEvents)

        assertEquals(
            listOf(MatrixEvent.Invite(roomId), MatrixEvent.Join(roomId, sender)),
            events,
        )
    }

    @Test
    fun `creates event from sync event`() {
        val roomId = "roomId"
        val sender = "sender"
        val message = "message"

        val syncCreate = Create(Create.Content(sender), sender = sender)
        val create = MatrixEvent.fromSync(roomId, syncCreate)

        assertEquals(MatrixEvent.Create(roomId, sender), create)

        val syncInvite = Member(Member.Content(Member.Membership.Invite))
        val invite = MatrixEvent.fromSync(roomId, syncInvite)

        assertEquals(MatrixEvent.Invite(roomId), invite)

        val syncJoin = Member(Member.Content(Member.Membership.Join), sender = sender)
        val join = MatrixEvent.fromSync(roomId, syncJoin)

        assertEquals(MatrixEvent.Join(roomId, sender), join)

        val syncTextMessage = Message(Message.Content(Message.TYPE_TEXT, message), sender = sender)
        val textMessage = MatrixEvent.fromSync(roomId, syncTextMessage)

        assertEquals(MatrixEvent.TextMessage(roomId, sender, message), textMessage)

        val syncUnknown = Member(Member.Content(Member.Membership.Ban))
        val unknown = MatrixEvent.fromSync(roomId, syncUnknown)

        assertNull(unknown, "Expected unknown sync event to be mapped to null")
    }
}