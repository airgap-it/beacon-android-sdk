package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.room

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MatrixCreateRoomRequest(
    val visibility: Visibility? = null,
    @SerialName("room_alias_name") val roomAliasName: String? = null,
    val name: String? = null,
    val topic: String? = null,
    val invite: List<String>? = null,
    @SerialName("room_version") val roomVersion: String? = null,
    val preset: Preset? = null,
    @SerialName("is_direct") val isDirect: Boolean? = null,
) {

    @Serializable
    enum class Visibility {
        @SerialName("public")
        Public,

        @SerialName("private")
        Private,
    }

    @Serializable
    enum class Preset {
        @SerialName("private_chat")
        PrivateChat,

        @SerialName("public_chat")
        PublicChat,

        @SerialName("trusted_private_chat")
        TrustedPrivateChat,
    }
}

@Serializable
internal data class MatrixCreateRoomResponse(@SerialName("room_id") val roomId: String? = null)