package it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.event

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.reflect.KClass

private typealias ISerializable = java.io.Serializable

@Serializable(with = MatrixStateEvent.Serializer::class)
internal sealed class MatrixStateEvent<Content : ISerializable> {
    abstract val content: Content?
    abstract val type: String?

    @SerialName("event_id")
    abstract val eventId: String?

    abstract val sender: String?

    @SerialName("state_key")
    abstract val stateKey: String?

    @Serializable
    data class Create(
        override val content: Content?,
        override val eventId: String?,
        override val sender: String?,
        override val stateKey: String?
    ) : MatrixStateEvent<Create.Content>() {
        override val type: String? = TYPE_CREATE

        @Serializable
        data class Content(val creator: String) : ISerializable
    }

    @Serializable
    data class Member(
        override val content: Content?,
        override val eventId: String?,
        override val sender: String?,
        override val stateKey: String?
    ) : MatrixStateEvent<Member.Content>() {
        override val type: String? = TYPE_MEMBER

        @Serializable
        data class Content(val membership: Membership?): ISerializable

        @Serializable
        enum class Membership {
            @SerialName("invite")
            Invite,

            @SerialName("join")
            Join,

            @SerialName("leave")
            Leave,

            @SerialName("ban")
            Ban,

            @SerialName("knock")
            Knock,
        }
    }

    @Serializable
    data class Message(
        override val content: Content?,
        override val eventId: String?,
        override val sender: String?,
        override val stateKey: String?
    ) : MatrixStateEvent<Message.Content>() {
        override val type: String? = TYPE_MESSAGE

        @Serializable
        data class Content(val msgtype: String?, val body: String?): ISerializable

        companion object {
            const val TYPE_TEXT = "m.text"
        }
    }

    @Serializable
    data class Unknown(
        override val content: String? = null,
        override val type: String? = null,
        override val eventId: String? = null,
        override val sender: String? = null,
        override val stateKey: String? = null,
    ) : MatrixStateEvent<String>()

    companion object {
        const val TYPE_CREATE = "m.room.create"
        const val TYPE_MEMBER = "m.room.member"
        const val TYPE_MESSAGE = "m.room.message"
    }

    object Serializer : KSerializer<MatrixStateEvent<*>> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("MatrixStateEvent", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): MatrixStateEvent<*> {
            val jsonDecoder = decoder as? JsonDecoder ?: failWithExpectedJsonDecoder(decoder::class)
            val jsonElement = jsonDecoder.decodeJsonElement()

            val type = jsonElement.jsonObject["type"]?.jsonPrimitive?.content

            return when (type) {
                TYPE_CREATE -> jsonDecoder.json.decodeFromJsonElement(Create.serializer(), jsonElement)
                TYPE_MEMBER -> jsonDecoder.json.decodeFromJsonElement(Member.serializer(), jsonElement)
                TYPE_MESSAGE -> jsonDecoder.json.decodeFromJsonElement(Message.serializer(), jsonElement)
                else -> jsonDecoder.json.decodeFromJsonElement(Unknown.serializer(), jsonElement)
            }
        }

        override fun serialize(encoder: Encoder, value: MatrixStateEvent<*>) {
            when (value) {
                is Create -> encoder.encodeSerializableValue(Create.serializer(), value)
                is Member -> encoder.encodeSerializableValue(Member.serializer(), value)
                is Message -> encoder.encodeSerializableValue(Message.serializer(), value)
                is Unknown -> encoder.encodeSerializableValue(Unknown.serializer(), value)
            }
        }

        private fun failWithExpectedJsonDecoder(actual: KClass<out Decoder>): Nothing =
            throw SerializationException("Expected Json decoder, got $actual")
    }
}