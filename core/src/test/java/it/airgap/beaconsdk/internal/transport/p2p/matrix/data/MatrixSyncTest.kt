package it.airgap.beaconsdk.internal.transport.p2p.matrix.data

import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncResponse
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncRoom
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncRooms
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncState
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncStateEvent.Member
import org.junit.Test
import kotlin.test.assertEquals

internal class MatrixSyncTest {

    @Test
    fun `constructs from sync response`() {
        val nextBatch = "nextBatch"

        val syncResponse = MatrixSyncResponse(
            nextBatch,
            MatrixSyncRooms(
                join = mapOf(
                    "1" to MatrixSyncRoom.Joined(
                        MatrixSyncState(
                            listOf(
                                Member(Member.Content(Member.Membership.Invite)),
                                Member(Member.Content(Member.Membership.Join), sender = "sender"),
                            )
                        )
                    )
                )
            )
        )

        val sync = MatrixSync.fromSyncResponse(syncResponse)

        assertEquals(
            MatrixSync(
                nextBatch,
                listOf(MatrixRoom.Joined("1", listOf("sender"))),
                listOf(MatrixEvent.Invite("1"), MatrixEvent.Join("1", "sender")),
            ),
            sync,
        )
    }
}