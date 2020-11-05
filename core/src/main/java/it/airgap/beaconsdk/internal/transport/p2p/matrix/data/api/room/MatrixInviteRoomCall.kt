package it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.room

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MatrixInviteRoomRequest(@SerialName("user_id") val userId: String? = null)

@Serializable
internal class MatrixInviteRoomResponse