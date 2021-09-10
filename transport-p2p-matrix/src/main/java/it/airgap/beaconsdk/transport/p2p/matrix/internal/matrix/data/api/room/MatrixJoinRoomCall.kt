package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.room

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public class MatrixJoinRoomRequest

@Serializable
public data class MatrixJoinRoomResponse(@SerialName("user_id") val userId: String? = null)