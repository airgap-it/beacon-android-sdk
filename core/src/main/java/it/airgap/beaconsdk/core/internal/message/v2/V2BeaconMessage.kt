package it.airgap.beaconsdk.core.internal.message.v2

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.internal.compat.CoreCompat
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
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
@Serializable(with = V2BeaconMessage.Serializer::class)
public abstract class V2BeaconMessage : VersionedBeaconMessage() {
    public abstract val id: String
    public abstract val type: String
    public abstract val senderId: String

    public companion object {
        public fun from(senderId: String, message: BeaconMessage): V2BeaconMessage =
            with(message) {
                when (this) {
                    is AcknowledgeBeaconResponse -> AcknowledgeV2BeaconResponse(version, id, senderId)
                    is ErrorBeaconResponse -> ErrorV2BeaconResponse(version, id, senderId, errorType)
                    is DisconnectBeaconMessage -> DisconnectV2BeaconMessage(version, id, senderId)
                    else -> CoreCompat.versioned.blockchain.creator.v2.from(senderId, message).getOrThrow()
                }
            }

        public fun compatSerializer(): KSerializer<V2BeaconMessage> = CoreCompat.versioned.blockchain.serializer.v2.message
    }

    @OptIn(ExperimentalSerializationApi::class)
    internal object Serializer : KJsonSerializer<V2BeaconMessage> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V2BeaconMessage") {
            element<String>("type")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V2BeaconMessage {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))

            return when (type) {
                AcknowledgeV2BeaconResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(AcknowledgeV2BeaconResponse.serializer(), jsonElement)
                ErrorV2BeaconResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(ErrorV2BeaconResponse.serializer(), jsonElement)
                DisconnectV2BeaconMessage.TYPE -> jsonDecoder.json.decodeFromJsonElement(DisconnectV2BeaconMessage.serializer(), jsonElement)
                else -> jsonDecoder.json.decodeFromJsonElement(compatSerializer(), jsonElement)
            }
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V2BeaconMessage) {
            when (value) {
                is AcknowledgeV2BeaconResponse -> jsonEncoder.encodeSerializableValue(AcknowledgeV2BeaconResponse.serializer(), value)
                is ErrorV2BeaconResponse -> jsonEncoder.encodeSerializableValue(ErrorV2BeaconResponse.serializer(), value)
                is DisconnectV2BeaconMessage -> jsonEncoder.encodeSerializableValue(DisconnectV2BeaconMessage.serializer(), value)
                else -> jsonEncoder.encodeSerializableValue(compatSerializer(), value)
            }
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
public data class AcknowledgeV2BeaconResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
) : V2BeaconMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        AcknowledgeBeaconResponse(id, version, origin, senderId)

    public companion object {
        public const val TYPE: String = "acknowledge"
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable(with = ErrorV2BeaconResponse.Serializer::class)
public data class ErrorV2BeaconResponse(
    override val version: String,
    override val id: String,
    override var senderId: String,
    val errorType: BeaconError,
) : V2BeaconMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        ErrorBeaconResponse(id, version, origin, errorType, CoreCompat.versioned.blockchain.identifier)

    public companion object {
        public const val TYPE: String = "error"
    }

    internal class Serializer : KJsonSerializer<ErrorV2BeaconResponse> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ErrorV2BeaconResponse") {
            element<String>("type")
            element<String>("version")
            element<String>("id")
            element<String>("senderId")
            element<BeaconError>("errorType")
        }

        override fun deserialize(
            jsonDecoder: JsonDecoder,
            jsonElement: JsonElement,
        ): ErrorV2BeaconResponse = jsonDecoder.decodeStructure(descriptor) {
            val version = decodeStringElement(descriptor, 1)
            val id = decodeStringElement(descriptor, 2)
            val senderId = decodeStringElement(descriptor, 3)
            val errorType = decodeSerializableElement(descriptor, 4, BeaconError.serializer(CoreCompat.versioned.blockchain.identifier))

            return ErrorV2BeaconResponse(version, id, senderId, errorType)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: ErrorV2BeaconResponse) {
            jsonEncoder.encodeStructure(descriptor) {
                with(value) {
                    encodeStringElement(descriptor, 0, type)
                    encodeStringElement(descriptor, 1, version)
                    encodeStringElement(descriptor, 2, id)
                    encodeStringElement(descriptor, 3, senderId)
                    encodeSerializableElement(descriptor, 4, BeaconError.serializer(CoreCompat.versioned.blockchain.identifier), errorType)
                }
            }
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
public data class DisconnectV2BeaconMessage(
    override val version: String,
    override val id: String,
    override var senderId: String,
) : V2BeaconMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        DisconnectBeaconMessage(id, senderId, version, origin)

    public companion object {
        public const val TYPE: String = "disconnect"
    }
}