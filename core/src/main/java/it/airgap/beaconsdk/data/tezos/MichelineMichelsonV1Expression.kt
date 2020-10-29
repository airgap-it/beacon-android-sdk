package it.airgap.beaconsdk.data.tezos

import it.airgap.beaconsdk.internal.utils.failWithExpectedJsonDecoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject

@Serializable(with = MichelineMichelsonV1Expression.Serializer::class)
public sealed class MichelineMichelsonV1Expression {

    public companion object {}

    internal class Serializer : KSerializer<MichelineMichelsonV1Expression> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("MichelineMichelsonV1Expression", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): MichelineMichelsonV1Expression {
            val jsonDecoder = decoder as? JsonDecoder ?: failWithExpectedJsonDecoder(decoder::class)
            val jsonElement = jsonDecoder.decodeJsonElement()

            val jsonObject = jsonElement.jsonObject

            return when {
                jsonObject.containsKey("int") -> jsonDecoder.json.decodeFromJsonElement(MichelinePrimitiveInt.serializer(), jsonElement)
                jsonObject.containsKey("string") -> jsonDecoder.json.decodeFromJsonElement(MichelinePrimitiveString.serializer(), jsonElement)
                jsonObject.containsKey("bytes") -> jsonDecoder.json.decodeFromJsonElement(MichelinePrimitiveBytes.serializer(), jsonElement)
                jsonObject.containsKey("expressions") -> jsonDecoder.json.decodeFromJsonElement(MichelineNode.serializer(), jsonElement)
                else -> jsonDecoder.json.decodeFromJsonElement(MichelinePrimitiveApplication.serializer(), jsonElement)
            }
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

@Serializable
public data class MichelinePrimitiveInt(public val int: String) : MichelineMichelsonV1Expression() {
    public companion object {}
}

@Serializable
public data class MichelinePrimitiveString(public val string: String) :
    MichelineMichelsonV1Expression() {
    public companion object {}
}

@Serializable
public data class MichelinePrimitiveBytes(public val bytes: String) :
    MichelineMichelsonV1Expression() {
    public companion object {}
}

@Serializable
public data class MichelinePrimitiveApplication(
    public val prim: String,
    public val args: List<MichelineMichelsonV1Expression>?,
    public val annots: List<String>?,
) : MichelineMichelsonV1Expression() {
    public companion object {}
}

@Serializable
public data class MichelineNode(public val expressions: List<MichelineMichelsonV1Expression>) :
    MichelineMichelsonV1Expression() {
    public companion object {}
}