package it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data

import it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data.api.sync.MatrixSyncRoom
import it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data.api.sync.MatrixSyncRooms
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public sealed class MatrixRoom {
    abstract val id: String
    abstract val members: List<String>

    @Serializable
    @SerialName("joined")
    data class Joined(override val id: String, override val members: List<String>) : MatrixRoom()

    @Serializable
    @SerialName("invited")
    data class Invited(override val id: String, override val members: List<String>) : MatrixRoom()

    @Serializable
    @SerialName("left")
    data class Left(override val id: String, override val members: List<String>) : MatrixRoom()

    @Serializable
    @SerialName("unknown")
    data class Unknown(override val id: String, override val members: List<String> = emptyList()) : MatrixRoom()

    fun hasMember(member: String): Boolean = members.contains(member)

    fun update(newMembers: List<String>): MatrixRoom =
        when (this) {
            is Joined -> copy(members = (members + newMembers).distinct())
            is Invited -> copy(members = (members + newMembers).distinct())
            is Left -> copy(members = (members + newMembers).distinct())
            is Unknown -> copy(members = (members + newMembers).distinct())
        }

    companion object {
        fun fromSync(node: String, syncRooms: MatrixSyncRooms): List<MatrixRoom> {
            val joined =
                syncRooms.join?.entries?.map { Joined(it.key, membersFromSync(node, it.key, it.value)) } ?: emptyList()

            val invited =
                syncRooms.invite?.entries?.map { Invited(it.key, membersFromSync(node, it.key, it.value)) } ?: emptyList()

            val left =
                syncRooms.leave?.entries?.map { Left(it.key, membersFromSync(node, it.key, it.value)) } ?: emptyList()

            return joined + invited + left
        }

        private fun membersFromSync(node: String, id: String, syncRoom: MatrixSyncRoom): List<String> =
            MatrixEvent.fromSync(node, id, syncRoom).filterIsInstance<MatrixEvent.Join>().map(MatrixEvent.Join::userId)
    }
}