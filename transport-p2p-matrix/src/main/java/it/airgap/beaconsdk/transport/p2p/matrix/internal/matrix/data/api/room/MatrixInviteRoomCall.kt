package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.room

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class MatrixInviteRoomRequest(@SerialName("user_id") val userId: String? = null)

@Serializable
public class MatrixInviteRoomResponse