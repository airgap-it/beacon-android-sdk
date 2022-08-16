package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.room

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MatrixInviteRoomRequest(@SerialName("user_id") val userId: String? = null)

@Serializable
internal class MatrixInviteRoomResponse {
    override fun equals(other: Any?): Boolean =
        when (other) {
            is MatrixInviteRoomResponse -> true
            else -> false
        }
}