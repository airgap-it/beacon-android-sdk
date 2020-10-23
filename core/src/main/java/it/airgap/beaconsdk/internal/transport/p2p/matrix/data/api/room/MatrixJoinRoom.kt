package it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.room

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class MatrixJoinRoomRequest

@Serializable
internal data class MatrixJoinRoomResponse(@SerialName("user_id") val userId: String?)