package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator(MatrixSyncRoom.CLASS_DISCRIMINATOR)
internal sealed class MatrixSyncRoom {

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

    companion object {
        const val CLASS_DISCRIMINATOR = "type"
    }
}

@Serializable
internal data class MatrixSyncRooms(
    val join: Map<String, MatrixSyncRoom.Joined>? = null,
    val invite: Map<String, MatrixSyncRoom.Invited>? = null,
    val leave: Map<String, MatrixSyncRoom.Left>? = null,
)