package it.airgap.beaconsdk.chain.tezos.data.operation

import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.failWithMissingField
import it.airgap.beaconsdk.core.internal.utils.failWithUnexpectedJsonType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
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

    public object Serializer : KJsonSerializer<MichelineMichelsonV1Expression> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MichelineMichelsonV1Expression")

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): MichelineMichelsonV1Expression {
            return when (jsonElement) {
                is JsonArray -> deserializeJsonArray(jsonDecoder, jsonElement)
                is JsonObject -> deserializeJsonObject(jsonDecoder, jsonElement)
                else -> failWithUnexpectedJsonType(jsonElement::class)
            }
        }

        private fun deserializeJsonArray(decoder: JsonDecoder, jsonArray: JsonArray): MichelineMichelsonV1Expression =
            decoder.json.decodeFromJsonElement(MichelineNode.serializer(), jsonArray)

        private fun deserializeJsonObject(decoder: JsonDecoder, jsonObject: JsonObject): MichelineMichelsonV1Expression =
            when {
                jsonObject.containsKey(MichelinePrimitiveInt.Serializer.Field.INT) -> decoder.json.decodeFromJsonElement(MichelinePrimitiveInt.serializer(), jsonObject)
                jsonObject.containsKey(MichelinePrimitiveString.Serializer.Field.STRING) -> decoder.json.decodeFromJsonElement(MichelinePrimitiveString.serializer(), jsonObject)
                jsonObject.containsKey(MichelinePrimitiveBytes.Serializer.Field.BYTES) -> decoder.json.decodeFromJsonElement(MichelinePrimitiveBytes.serializer(), jsonObject)
                jsonObject.containsKey(MichelineNode.Serializer.Field.EXPRESSIONS) -> decoder.json.decodeFromJsonElement(MichelineNode.serializer(), jsonObject)
                else -> decoder.json.decodeFromJsonElement(MichelinePrimitiveApplication.serializer(), jsonObject)
            }

        override fun serialize(jsonEncoder: JsonEncoder, value: MichelineMichelsonV1Expression) {
            when (value) {
                is MichelinePrimitiveInt -> jsonEncoder.encodeSerializableValue(MichelinePrimitiveInt.serializer(), value)
                is MichelinePrimitiveString -> jsonEncoder.encodeSerializableValue(MichelinePrimitiveString.serializer(), value)
                is MichelinePrimitiveBytes -> jsonEncoder.encodeSerializableValue(MichelinePrimitiveBytes.serializer(), value)
                is MichelinePrimitiveApplication -> jsonEncoder.encodeSerializableValue(MichelinePrimitiveApplication.serializer(), value)
                is MichelineNode -> jsonEncoder.encodeSerializableValue(MichelineNode.serializer(), value)
            }
        }
    }
}

@Serializable(with = MichelinePrimitiveInt.Serializer::class)
public data class MichelinePrimitiveInt(public val int: Int) : MichelineMichelsonV1Expression() {
    public companion object {}

    internal object Serializer : KSerializer<MichelinePrimitiveInt> {
        object Field {
            const val INT = "int"
        }

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MichelinePrimitiveInt") {
            element<String>(Field.INT)
        }

        override fun deserialize(decoder: Decoder): MichelinePrimitiveInt =
            decoder.decodeStructure(descriptor) {
                val primitive = decodeStringElement(descriptor, 0).toInt()

                return MichelinePrimitiveInt(primitive)
            }

        override fun serialize(encoder: Encoder, value: MichelinePrimitiveInt) {
            encoder.encodeStructure(descriptor) {
                with(value) {
                    encodeStringElement(descriptor, 0, int.toString())
                }
            }
        }
    }
}

@Serializable
public data class MichelinePrimitiveString(public val string: String) : MichelineMichelsonV1Expression() {
    public companion object {}

    internal object Serializer {
        object Field {
            const val STRING = "string"
        }
    }
}

@Serializable
public data class MichelinePrimitiveBytes(public val bytes: String) : MichelineMichelsonV1Expression() {
    public companion object {}

    internal object Serializer {
        object Field {
            const val BYTES = "bytes"
        }
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

    internal object Serializer : KJsonSerializer<MichelineNode> {
        object Field {
            const val EXPRESSIONS = "expressions"
        }

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MichelineNode") {
            element<List<MichelineMichelsonV1Expression>>(Field.EXPRESSIONS)
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): MichelineNode {
            return when (jsonElement) {
                is JsonArray -> deserializeJsonArray(jsonDecoder, jsonElement)
                is JsonObject -> deserializeJsonObject(jsonDecoder, jsonElement)
                else -> failWithUnexpectedJsonType(jsonElement::class)
            }
        }

        private fun deserializeJsonArray(decoder: JsonDecoder, jsonArray: JsonArray): MichelineNode {
            val expressions = jsonArray.map { decoder.json.decodeFromJsonElement(
                MichelineMichelsonV1Expression.serializer(), it) }

            return MichelineNode(expressions)
        }

        private fun deserializeJsonObject(decoder: JsonDecoder, jsonObject: JsonObject): MichelineNode {
            val expressions = jsonObject[Field.EXPRESSIONS]?.jsonArray ?: failWithMissingField(Field.EXPRESSIONS)

            return deserializeJsonArray(decoder, expressions)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: MichelineNode) {
            val jsonArray = JsonArray(value.expressions.map { jsonEncoder.json.encodeToJsonElement(
                MichelineMichelsonV1Expression.serializer(), it) })

            jsonEncoder.encodeJsonElement(jsonArray)
        }
    }
}