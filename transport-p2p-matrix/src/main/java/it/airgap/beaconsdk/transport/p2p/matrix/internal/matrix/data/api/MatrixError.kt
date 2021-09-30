package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api

import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.getString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.jsonObject

@Serializable(with = MatrixError.Serializer::class)
internal sealed class MatrixError : Exception() {
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

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer : KJsonSerializer<MatrixError> {
        object Field {
            const val ERRCODE = "errcode"
        }

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MatrixError") {
            element<String>(Field.ERRCODE)
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): MatrixError {
            val errcode = jsonElement.jsonObject.getString(descriptor.getElementName(0))

            return when (errcode) {
                Forbidden.CODE -> jsonDecoder.json.decodeFromJsonElement(Forbidden.serializer(), jsonElement)
                else -> jsonDecoder.json.decodeFromJsonElement(Other.serializer(), jsonElement)
            }
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: MatrixError) {
            when (value) {
                is Forbidden -> jsonEncoder.encodeSerializableValue(Forbidden.serializer(), value)
                is Other -> jsonEncoder.encodeSerializableValue(Other.serializer(), value)
            }
        }
    }
}