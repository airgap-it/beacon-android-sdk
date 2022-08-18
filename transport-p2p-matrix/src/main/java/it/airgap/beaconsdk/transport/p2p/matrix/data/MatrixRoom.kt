package it.airgap.beaconsdk.transport.p2p.matrix.data

import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.MatrixEvent
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncRoom
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncRooms
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * Base for types of [Matrix](https://matrix.org/) rooms supported in Beacon.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator(MatrixRoom.CLASS_DISCRIMINATOR)
public sealed class MatrixRoom {
    public abstract val id: String
    public abstract val members: List<String>

    /**
     * A room the client has joined.
     *
     * @property [id] The ID of the room.
     * @property [members] A list of members that has joined the room.
     */
    @Serializable
    @SerialName("joined")
    public data class Joined(override val id: String, override val members: List<String>) : MatrixRoom()

    /**
     * A room the client has been invited to.
     *
     * @property [id] The ID of the room.
     * @property [members] A list of members that has joined the room.
     */
    @Serializable
    @SerialName("invited")
    public data class Invited(override val id: String, override val members: List<String>) : MatrixRoom()

    /**
     * A room the client has left.
     *
     * @property [id] The ID of the room.
     * @property [members] A list of members that has joined the room.
     */
    @Serializable
    @SerialName("left")
    public data class Left(override val id: String, override val members: List<String>) : MatrixRoom()

    /**
     * A room with an unknown relation to the client.
     *
     * @property [id] The ID of the room.
     * @property [members] A list of members that has joined the room.
     */
    @Serializable
    @SerialName("unknown")
    public data class Unknown(override val id: String, override val members: List<String> = emptyList()) : MatrixRoom()

    internal fun hasMember(member: String): Boolean = members.contains(member)

    internal fun update(newMembers: List<String>): MatrixRoom =
        when (this) {
            is Joined -> copy(members = (members + newMembers).distinct())
            is Invited -> copy(members = (members + newMembers).distinct())
            is Left -> copy(members = (members + newMembers).distinct())
            is Unknown -> copy(members = (members + newMembers).distinct())
        }

    public companion object {
        internal const val CLASS_DISCRIMINATOR = "type"

        internal fun fromSync(node: String, syncRooms: MatrixSyncRooms): List<MatrixRoom> {
            val joined =
                syncRooms.join?.entries?.map { Joined(it.key, membersFromSync(node, it.key, it.value)) } ?: emptyList()

            val invited =
                syncRooms.invite?.entries?.map { Invited(it.key, membersFromSync(node, it.key, it.value)) } ?: emptyList()

            val left =
                syncRooms.leave?.entries?.map { Left(it.key, membersFromSync(node, it.key, it.value)) } ?: emptyList()

            return joined + invited + left
        }

        private fun membersFromSync(node: String, id: String, syncRoom: MatrixSyncRoom): List<String> =
            MatrixEvent.fromSync(node, id, syncRoom)
                .filterIsInstance<MatrixEvent.Join>().map(MatrixEvent.Join::userId)
    }
}