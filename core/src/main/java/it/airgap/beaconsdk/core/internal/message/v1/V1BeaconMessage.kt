package it.airgap.beaconsdk.core.internal.message.v1

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.internal.compat.CoreCompat
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.failWithUnsupportedMessage
import it.airgap.beaconsdk.core.internal.utils.getString
import it.airgap.beaconsdk.core.message.AcknowledgeBeaconResponse
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.DisconnectBeaconMessage
import it.airgap.beaconsdk.core.message.ErrorBeaconResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.jsonObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable(with = V1BeaconMessage.Serializer::class)
public abstract class V1BeaconMessage : VersionedBeaconMessage() {
    public abstract val id: String
    public abstract val type: String
    public abstract val beaconId: String

    public companion object {
        public fun from(senderId: String, message: BeaconMessage): V1BeaconMessage =
            with(message) {
                when (this) {
                    is AcknowledgeBeaconResponse -> failWithUnsupportedMessage(message, version)
                    is ErrorBeaconResponse -> ErrorV1BeaconResponse(version, id, senderId, errorType)
                    is DisconnectBeaconMessage -> DisconnectV1BeaconMessage(version, id, senderId)
                    else -> CoreCompat.versioned.blockchain.creator.v1.from(senderId, message).getOrThrow()
                }
            }

        public fun compatSerializer(): KSerializer<V1BeaconMessage> = CoreCompat.versioned.blockchain.serializer.v1.message
    }

    @OptIn(ExperimentalSerializationApi::class)
    internal object Serializer : KJsonSerializer<V1BeaconMessage> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V1BeaconMessage") {
            element<String>("type")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V1BeaconMessage {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))

            return when (type) {
                ErrorV1BeaconResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(ErrorV1BeaconResponse.serializer(), jsonElement)
                DisconnectV1BeaconMessage.TYPE -> jsonDecoder.json.decodeFromJsonElement(DisconnectV1BeaconMessage.serializer(), jsonElement)
                else -> jsonDecoder.json.decodeFromJsonElement(compatSerializer(), jsonElement)
            }
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V1BeaconMessage) {
            when (value) {
                is ErrorV1BeaconResponse -> jsonEncoder.encodeSerializableValue(ErrorV1BeaconResponse.serializer(), value)
                is DisconnectV1BeaconMessage -> jsonEncoder.encodeSerializableValue(DisconnectV1BeaconMessage.serializer(), value)
                else -> jsonEncoder.encodeSerializableValue(compatSerializer(), value)
            }
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable(with = ErrorV1BeaconResponse.Serializer::class)
public data class ErrorV1BeaconResponse(
    override val version: String,
    override val id: String,
    override var beaconId: String,
    val errorType: BeaconError,
) : V1BeaconMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin): BeaconMessage =
        ErrorBeaconResponse(id, version, origin, errorType, null)

    public companion object {
        internal const val TYPE: String = "error"
    }

    internal class Serializer : KJsonSerializer<ErrorV1BeaconResponse> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ErrorV1BeaconResponse") {
            element<String>("type")
            element<String>("version")
            element<String>("id")
            element<String>("beaconId")
            element<BeaconError>("errorType")
        }

        override fun deserialize(
            jsonDecoder: JsonDecoder,
            jsonElement: JsonElement,
        ): ErrorV1BeaconResponse = jsonDecoder.decodeStructure(descriptor) {
            val version = decodeStringElement(descriptor, 1)
            val id = decodeStringElement(descriptor, 2)
            val beaconId = decodeStringElement(descriptor, 3)
            val errorType = decodeSerializableElement(descriptor, 4, BeaconError.serializer(CoreCompat.versioned.blockchain.identifier))

            return ErrorV1BeaconResponse(version, id, beaconId, errorType)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: ErrorV1BeaconResponse) {
            jsonEncoder.encodeStructure(descriptor) {
                with(value) {
                    encodeStringElement(descriptor, 0, type)
                    encodeStringElement(descriptor, 1, version)
                    encodeStringElement(descriptor, 2, id)
                    encodeStringElement(descriptor, 3, beaconId)
                    encodeSerializableElement(descriptor, 4, BeaconError.serializer(CoreCompat.versioned.blockchain.identifier), errorType)
                }
            }
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
public data class DisconnectV1BeaconMessage(
    override val version: String,
    override val id: String,
    override var beaconId: String,
) : V1BeaconMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin): BeaconMessage =
        DisconnectBeaconMessage(id, beaconId, version, origin)

    public companion object {
        internal const val TYPE: String = "disconnect"
    }
}