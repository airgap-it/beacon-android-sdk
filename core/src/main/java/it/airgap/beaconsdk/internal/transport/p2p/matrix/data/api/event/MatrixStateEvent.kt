package it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.event

import it.airgap.beaconsdk.internal.utils.failWithExpectedJsonDecoder
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.reflect.KClass

@Serializable(with = MatrixStateEvent.Serializer::class)
internal sealed class MatrixStateEvent<Content> {
    abstract val content: Content?
    abstract val type: String?
    abstract val eventId: String?
    abstract val sender: String?
    abstract val stateKey: String?

    @Serializable
    data class Create(
        override val content: Content?,
        @SerialName("event_id") override val eventId: String? = null,
        override val sender: String? = null,
        @SerialName("state_key") override val stateKey: String? = null,
    ) : MatrixStateEvent<Create.Content>() {
        override val type: String? = TYPE_CREATE

        @Serializable
        data class Content(val creator: String)
    }

    @Serializable
    data class Member(
        override val content: Content?,
        @SerialName("event_id") override val eventId: String? = null,
        override val sender: String? = null,
        @SerialName("state_key") override val stateKey: String? = null,
    ) : MatrixStateEvent<Member.Content>() {
        override val type: String? = TYPE_MEMBER

        @Serializable
        data class Content(val membership: Membership? = null)

        @Serializable
        enum class Membership {
            @SerialName("invite") Invite,
            @SerialName("join") Join,
            @SerialName("leave") Leave,
            @SerialName("ban") Ban,
            @SerialName("knock") Knock,
        }
    }

    @Serializable
    data class Message(
        override val content: Content?,
        @SerialName("event_id") override val eventId: String? = null,
        override val sender: String? = null,
        @SerialName("state_key") override val stateKey: String? = null,
    ) : MatrixStateEvent<Message.Content>() {
        override val type: String? = TYPE_MESSAGE

        @Serializable
        data class Content(val msgtype: String? = null, val body: String? = null)

        companion object {
            const val TYPE_TEXT = "m.text"
        }
    }

    @Serializable
    data class Unknown(
        override val content: JsonElement? = null,
        override val type: String? = null,
        @SerialName("event_id") override val eventId: String? = null,
        override val sender: String? = null,
        @SerialName("state_key") override val stateKey: String? = null,
    ) : MatrixStateEvent<JsonElement>()

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
    }
}