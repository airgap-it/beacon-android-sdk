package it.airgap.beaconsdk.internal.transport.p2p.matrix.data.client

import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncResponse
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncRoom
import kotlinx.serialization.Serializable

// TODO: resolve internal/public conflict (`MatrixRoom` is clearly an internal structure, but has to be exposed in the storage)
@Serializable
sealed class MatrixRoom {
    abstract val id: String
    abstract val members: List<String>

    @Serializable
    data class Joined(override val id: String, override val members: List<String>) : MatrixRoom()

    @Serializable
    data class Invited(override val id: String, override val members: List<String>) : MatrixRoom()

    @Serializable
    data class Left(override val id: String, override val members: List<String>) : MatrixRoom()

    @Serializable
    data class Unknown(override val id: String, override val members: List<String> = emptyList()) : MatrixRoom()

    internal fun update(newMembers: List<String>): MatrixRoom =
        when (this) {
            is Joined -> copy(members = (members + newMembers).distinct())
            is Invited -> copy(members = (members + newMembers).distinct())
            is Left -> copy(members = (members + newMembers).distinct())
            is Unknown -> copy(members = (members + newMembers).distinct())
        }

    companion object {
        internal fun fromSync(syncRooms: MatrixSyncResponse.Rooms): List<MatrixRoom> {
            val joined = syncRooms.join?.entries?.map { Joined(it.key, membersFromSync(it.key, it.value)) } ?: emptyList()
            val invited = syncRooms.invite?.entries?.map { Invited(it.key, membersFromSync(it.key, it.value)) } ?: emptyList()
            val left = syncRooms.leave?.entries?.map { Left(it.key, membersFromSync(it.key, it.value)) } ?: emptyList()

            return joined + invited + left
        }

        private fun membersFromSync(id: String, syncRoom: MatrixSyncRoom): List<String> =
            MatrixEvent.fromSync(id, syncRoom).filterIsInstance<MatrixEvent.Join>().map(MatrixEvent.Join::userId)
    }
}