package it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data.api

import it.airgap.beaconsdk.core.internal.utils.failWithExpectedJsonDecoder
import it.airgap.beaconsdk.core.internal.utils.failWithMissingField
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = MatrixError.Serializer::class)
public sealed class MatrixError : Exception() {
    abstract val code: String
    abstract val error: String

    @Serializable
    data class Forbidden(override val error: String) : MatrixError() {
        override val code: String = CODE

        companion object {
            const val CODE = "M_FORBIDDEN"
        }
    }

    @Serializable
    data class Other(@SerialName(Serializer.Field.ERRCODE) override val code: String, override val error: String) : MatrixError()

    object Serializer : KSerializer<MatrixError> {
        object Field {
            const val ERRCODE = "errcode"
        }

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("MatrixError", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): MatrixError {
            val jsonDecoder = decoder as? JsonDecoder ?: failWithExpectedJsonDecoder(decoder::class)
            val jsonElement = jsonDecoder.decodeJsonElement()

            val errcodeSerialized = jsonElement.jsonObject[Field.ERRCODE]?.jsonPrimitive ?: failWithMissingField(Field.ERRCODE)
            val errcode = jsonDecoder.json.decodeFromJsonElement(String.serializer(), errcodeSerialized)

            return when (errcode) {
                Forbidden.CODE -> jsonDecoder.json.decodeFromJsonElement(Forbidden.serializer(), jsonElement)
                else -> jsonDecoder.json.decodeFromJsonElement(Other.serializer(), jsonElement)
            }
        }

        override fun serialize(encoder: Encoder, value: MatrixError) {
            when (value) {
                is Forbidden -> encoder.encodeSerializableValue(Forbidden.serializer(), value)
                is Other -> encoder.encodeSerializableValue(Other.serializer(), value)
            }
        }
    }
}