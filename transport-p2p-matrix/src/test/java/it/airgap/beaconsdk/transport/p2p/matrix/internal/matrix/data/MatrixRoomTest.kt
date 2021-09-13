package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data

import it.airgap.beaconsdk.transport.p2p.matrix.data.MatrixRoom
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncRoom
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncRooms
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncState
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncStateEvent.Create
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncStateEvent.Member
import org.junit.Test
import kotlin.test.assertEquals

internal class MatrixRoomTest {

    @Test
    fun `updates with new members`() {
        val oldMembers = listOf("1", "2")
        val newMembers = listOf("3", "4")

        val join = MatrixRoom.Joined("1", oldMembers)
        val invited = MatrixRoom.Invited("2", oldMembers)
        val left = MatrixRoom.Left("3", oldMembers)
        val unknown = MatrixRoom.Unknown("4", oldMembers)

        val joinUpdated = join.update(newMembers)
        val invitedUpdated = invited.update(newMembers)
        val leftUpdated = left.update(newMembers)
        val unknownUpdated = unknown.update(newMembers)

        assertEquals(MatrixRoom.Joined(join.id, join.members + newMembers), joinUpdated)
        assertEquals(MatrixRoom.Invited(invited.id, invited.members + newMembers), invitedUpdated)
        assertEquals(MatrixRoom.Left(left.id, left.members + newMembers), leftUpdated)
        assertEquals(MatrixRoom.Unknown(unknown.id, unknown.members + newMembers), unknownUpdated)
    }

    @Test
    fun `removes duplicated members on update`() {
        val updated = MatrixRoom.Joined("1", listOf("1", "2")).update(listOf("2", "3"))

        assertEquals(listOf("1", "2", "3"), updated.members)
    }

    @Test
    fun `creates list of rooms from sync rooms response`() {
        val node = "node"
        val syncRooms = MatrixSyncRooms(
            join = mapOf(
                "1" to MatrixSyncRoom.Joined(
                    MatrixSyncState(
                        listOf(
                            Member(Member.Content(Member.Membership.Join), sender = "member#1"),
                            Member(Member.Content(Member.Membership.Leave), sender = "member#2"),
                            Member(Member.Content(Member.Membership.Invite), sender = "member#3"),
                        )
                    )
                ),
                "2" to MatrixSyncRoom.Joined(),
            ),
            invite = mapOf(
                "3" to MatrixSyncRoom.Invited(
                    MatrixSyncState(
                        listOf(
                            Member(Member.Content(Member.Membership.Ban), sender = "member#4"),
                            Member(Member.Content(Member.Membership.Knock), sender = "member#5"),
                            Member(Member.Content(Member.Membership.Join), sender = "member#6"),
                        )
                    )
                )
            ),
            leave = mapOf(
                "4" to MatrixSyncRoom.Left(
                    MatrixSyncState(
                        listOf(
                            Create(Create.Content("creator#1")),
                            Member(Member.Content(Member.Membership.Join), sender = "member#7"),
                        )
                    )
                )
            )
        )

        val rooms = MatrixRoom.fromSync(node, syncRooms)

        assertEquals(
            listOf(
                MatrixRoom.Joined("1", listOf("member#1")),
                MatrixRoom.Joined("2", emptyList()),
                MatrixRoom.Invited("3", listOf("member#6")),
                MatrixRoom.Left("4", listOf("member#7")),
            ),
            rooms,
        )
    }
}