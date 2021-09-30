package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data

import it.airgap.beaconsdk.transport.p2p.matrix.data.MatrixRoom
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncResponse
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncRoom
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncRooms
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncState
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncStateEvent.Member
import org.junit.Test
import kotlin.test.assertEquals

internal class MatrixSyncTest {

    @Test
    fun `constructs from sync response`() {
        val node = "node"
        val sender = "sender"
        val nextBatch = "nextBatch"

        val syncResponse = MatrixSyncResponse(
            nextBatch,
            MatrixSyncRooms(
                join = mapOf(
                    "1" to MatrixSyncRoom.Joined(
                        MatrixSyncState(
                            listOf(
                                Member(Member.Content(Member.Membership.Invite), sender = sender),
                                Member(Member.Content(Member.Membership.Join), sender = sender),
                            )
                        )
                    )
                )
            )
        )

        val sync = MatrixSync.fromSyncResponse(node, syncResponse)

        assertEquals(
            MatrixSync(
                nextBatch,
                listOf(MatrixRoom.Joined("1", listOf(sender))),
                listOf(MatrixEvent.Invite(node, "1", sender), MatrixEvent.Join(node, "1", sender)),
            ),
            sync,
        )
    }
}