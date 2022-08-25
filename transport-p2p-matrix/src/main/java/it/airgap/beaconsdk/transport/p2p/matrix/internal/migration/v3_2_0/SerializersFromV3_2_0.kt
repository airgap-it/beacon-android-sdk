@file:OptIn(ExperimentalSerializationApi::class)

package it.airgap.beaconsdk.transport.p2p.matrix.internal.migration.v3_2_0

import it.airgap.beaconsdk.core.internal.serializer.BEACON_CORE_CLASS_DISCRIMINATOR
import it.airgap.beaconsdk.transport.p2p.matrix.data.MatrixRoom
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonNames

private const val v3_2_0_BEACON_CORE_CLASS_DISCRIMINATOR = BEACON_CORE_CLASS_DISCRIMINATOR

// -- MatrixRoom --

internal class MatrixRoomSerializer : KSerializer<MatrixRoom> {
    override val descriptor: SerialDescriptor = MatrixRoomSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): MatrixRoom {
        val surrogate = decoder.decodeSerializableValue(MatrixRoomSurrogate.serializer())
        return surrogate.toTarget()
    }

    override fun serialize(encoder: Encoder, value: MatrixRoom) {
        val surrogate = MatrixRoomSurrogate(value)
        encoder.encodeSerializableValue(MatrixRoomSurrogate.serializer(), surrogate)
    }
}

@Serializable
private data class MatrixRoomSurrogate(
    @JsonNames(v3_2_0_BEACON_CORE_CLASS_DISCRIMINATOR) val type: Type,
    val id: String,
    val members: List<String>,
) {
    fun toTarget(): MatrixRoom = when (type) {
        Type.Joined -> MatrixRoom.Joined(id, members)
        Type.Invited -> MatrixRoom.Invited(id, members)
        Type.Left -> MatrixRoom.Left(id, members)
        Type.Unknown -> MatrixRoom.Unknown(id, members)
    }

    @Serializable
    enum class Type {
        @SerialName(MatrixRoom.Joined.TYPE) Joined,
        @SerialName(MatrixRoom.Invited.TYPE) Invited,
        @SerialName(MatrixRoom.Left.TYPE) Left,
        @SerialName(MatrixRoom.Unknown.TYPE) Unknown,
    }
}

private fun MatrixRoomSurrogate(value: MatrixRoom): MatrixRoomSurrogate = with(value) {
    val type = when (this) {
        is MatrixRoom.Joined -> MatrixRoomSurrogate.Type.Joined
        is MatrixRoom.Invited -> MatrixRoomSurrogate.Type.Invited
        is MatrixRoom.Left -> MatrixRoomSurrogate.Type.Left
        is MatrixRoom.Unknown -> MatrixRoomSurrogate.Type.Unknown
    }

    return MatrixRoomSurrogate(type, id, members)
}