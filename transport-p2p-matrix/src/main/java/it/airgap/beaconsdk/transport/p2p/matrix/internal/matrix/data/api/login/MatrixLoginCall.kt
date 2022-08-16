package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.login

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator(MatrixLoginRequest.CLASS_DISCRIMINATOR)
internal sealed class MatrixLoginRequest {
    abstract val identifier: UserIdentifier
    abstract val deviceId: String?

    @Serializable
    @SerialName(Password.TYPE)
    data class Password(
        override val identifier: UserIdentifier,
        val password: String,
        @SerialName("device_id") override val deviceId: String? = null,
    ) : MatrixLoginRequest() {

        companion object {
            const val TYPE = "m.login.password"
        }
    }

    @Serializable
    @JsonClassDiscriminator(UserIdentifier.CLASS_DISCRIMINATOR)
    sealed class UserIdentifier {

        @Serializable
        @SerialName(User.TYPE)
        data class User(val user: String) : UserIdentifier() {
            companion object {
                const val TYPE = "m.id.user"
            }
        }

        companion object {
            const val CLASS_DISCRIMINATOR = "type"
        }
    }

    companion object {
        const val CLASS_DISCRIMINATOR = "type"
    }
}

@Serializable
internal data class MatrixLoginResponse(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("access_token") val accessToken: String? = null,
)