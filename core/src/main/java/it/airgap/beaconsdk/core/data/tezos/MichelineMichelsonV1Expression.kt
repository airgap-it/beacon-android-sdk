package it.airgap.beaconsdk.core.data.tezos

import it.airgap.beaconsdk.core.internal.utils.failWithExpectedJsonDecoder
import it.airgap.beaconsdk.core.internal.utils.failWithExpectedJsonEncoder
import it.airgap.beaconsdk.core.internal.utils.failWithMissingField
import it.airgap.beaconsdk.core.internal.utils.failWithUnexpectedJsonType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * Base for JSON Micheline expressions.
 *
 * @see [Micheline White Doc](https://tezos.gitlab.io/whitedoc/micheline.html)
 * @see [Michelson White Doc](https://tezos.gitlab.io/whitedoc/michelson.html)
 */
@Serializable(with = MichelineMichelsonV1Expression.Serializer::class)
public sealed class MichelineMichelsonV1Expression {

    public companion object {}

    internal object Serializer : KSerializer<MichelineMichelsonV1Expression> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("MichelineMichelsonV1Expression", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): MichelineMichelsonV1Expression {
            val jsonDecoder = decoder as? JsonDecoder ?: failWithExpectedJsonDecoder(decoder::class)

            return when (val jsonElement = jsonDecoder.decodeJsonElement()) {
                is JsonArray -> deserializeJsonArray(jsonDecoder, jsonElement)
                is JsonObject -> deserializeJsonObject(jsonDecoder, jsonElement)
                else -> failWithUnexpectedJsonType(jsonElement::class)
            }
        }

        private fun deserializeJsonArray(decoder: JsonDecoder, jsonArray: JsonArray): MichelineMichelsonV1Expression =
            decoder.json.decodeFromJsonElement(MichelineNode.serializer(), jsonArray)

        private fun deserializeJsonObject(decoder: JsonDecoder, jsonObject: JsonObject): MichelineMichelsonV1Expression =
            when {
                jsonObject.containsKey(MichelinePrimitiveInt.Field.INT) -> decoder.json.decodeFromJsonElement(MichelinePrimitiveInt.serializer(), jsonObject)
                jsonObject.containsKey(MichelinePrimitiveString.Field.STRING) -> decoder.json.decodeFromJsonElement(MichelinePrimitiveString.serializer(), jsonObject)
                jsonObject.containsKey(MichelinePrimitiveBytes.Field.BYTES) -> decoder.json.decodeFromJsonElement(MichelinePrimitiveBytes.serializer(), jsonObject)
                jsonObject.containsKey(MichelineNode.Field.EXPRESSIONS) -> decoder.json.decodeFromJsonElement(MichelineNode.serializer(), jsonObject)
                else -> decoder.json.decodeFromJsonElement(MichelinePrimitiveApplication.serializer(), jsonObject)
            }

        override fun serialize(encoder: Encoder, value: MichelineMichelsonV1Expression) {
            when (value) {
                is MichelinePrimitiveInt -> encoder.encodeSerializableValue(MichelinePrimitiveInt.serializer(), value)
                is MichelinePrimitiveString -> encoder.encodeSerializableValue(MichelinePrimitiveString.serializer(), value)
                is MichelinePrimitiveBytes -> encoder.encodeSerializableValue(MichelinePrimitiveBytes.serializer(), value)
                is MichelinePrimitiveApplication -> encoder.encodeSerializableValue(MichelinePrimitiveApplication.serializer(), value)
                is MichelineNode -> encoder.encodeSerializableValue(MichelineNode.serializer(), value)
            }
        }
    }
}

@Serializable(with = MichelinePrimitiveInt.Serializer::class)
public data class MichelinePrimitiveInt(public val int: Int) : MichelineMichelsonV1Expression() {
    public companion object {}

    internal object Field {
        const val INT = "int"
    }

    internal object Serializer : KSerializer<MichelinePrimitiveInt> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("MichelineNode", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): MichelinePrimitiveInt {
            val jsonDecoder = decoder as? JsonDecoder ?: failWithExpectedJsonDecoder(decoder::class)
            val jsonElement = jsonDecoder.decodeJsonElement()

            val primitive = jsonElement.jsonObject[Field.INT]?.jsonPrimitive ?: failWithMissingField(Field.INT)

            return MichelinePrimitiveInt(primitive.content.toInt())
        }

        override fun serialize(encoder: Encoder, value: MichelinePrimitiveInt) {
            val jsonEncoder = encoder as? JsonEncoder ?: failWithExpectedJsonEncoder(encoder::class)
            val jsonObject = JsonObject(
                mapOf(Field.INT to JsonPrimitive(value.int.toString()))
            )
            jsonEncoder.encodeJsonElement(jsonObject)
        }
    }
}

@Serializable
public data class MichelinePrimitiveString(public val string: String) : MichelineMichelsonV1Expression() {
    public companion object {}

    internal object Field {
        const val STRING = "string"
    }
}

@Serializable
public data class MichelinePrimitiveBytes(public val bytes: String) : MichelineMichelsonV1Expression() {
    public companion object {}

    internal object Field {
        const val BYTES = "bytes"
    }
}

@Serializable
public data class MichelinePrimitiveApplication(
    public val prim: String,
    public val args: List<MichelineMichelsonV1Expression>? = null,
    public val annots: List<String>? = null,
) : MichelineMichelsonV1Expression() {
    public companion object {}
}

@Serializable(with = MichelineNode.Serializer::class)
public data class MichelineNode(public val expressions: List<MichelineMichelsonV1Expression>) : MichelineMichelsonV1Expression() {
    public companion object {}

    internal object Field {
        const val EXPRESSIONS = "expressions"
    }

    internal object Serializer : KSerializer<MichelineNode> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("MichelineNode", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): MichelineNode {
            val jsonDecoder = decoder as? JsonDecoder ?: failWithExpectedJsonDecoder(decoder::class)

            return when (val jsonElement = jsonDecoder.decodeJsonElement()) {
                is JsonArray -> deserializeJsonArray(jsonDecoder, jsonElement)
                is JsonObject -> deserializeJsonObject(jsonDecoder, jsonElement)
                else -> failWithUnexpectedJsonType(jsonElement::class)
            }
        }

        private fun deserializeJsonArray(decoder: JsonDecoder, jsonArray: JsonArray): MichelineNode {
            val expressions = jsonArray.map { decoder.json.decodeFromJsonElement(MichelineMichelsonV1Expression.serializer(), it) }

            return MichelineNode(expressions)
        }

        private fun deserializeJsonObject(decoder: JsonDecoder, jsonObject: JsonObject): MichelineNode {
            val expressions = jsonObject[Field.EXPRESSIONS]?.jsonArray ?: failWithMissingField(Field.EXPRESSIONS)

            return deserializeJsonArray(decoder, expressions)
        }

        override fun serialize(encoder: Encoder, value: MichelineNode) {
            val jsonEncoder = encoder as? JsonEncoder ?: failWithExpectedJsonEncoder(encoder::class)
            val jsonArray = JsonArray(value.expressions.map { jsonEncoder.json.encodeToJsonElement(MichelineMichelsonV1Expression.serializer(), it) })

            jsonEncoder.encodeJsonElement(jsonArray)
        }
    }
}