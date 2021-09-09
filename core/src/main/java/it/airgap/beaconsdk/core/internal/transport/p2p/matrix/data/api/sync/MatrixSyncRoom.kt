package it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data.api.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public sealed class MatrixSyncRoom {

    @Serializable
    @SerialName("joined")
    data class Joined(
        val state: MatrixSyncState? = null,
        val timeline: MatrixSyncTimeline? = null,
    ) : MatrixSyncRoom()

    @Serializable
    @SerialName("invited")
    data class Invited(@SerialName("invite_state") val state: MatrixSyncState? = null) : MatrixSyncRoom()

    @Serializable
    @SerialName("left")
    data class Left(val state: MatrixSyncState? = null, val timeline: MatrixSyncTimeline? = null) : MatrixSyncRoom()
}

@Serializable
public data class MatrixSyncRooms(
    val join: Map<String, MatrixSyncRoom.Joined>? = null,
    val invite: Map<String, MatrixSyncRoom.Invited>? = null,
    val leave: Map<String, MatrixSyncRoom.Left>? = null,
)