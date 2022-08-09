package it.airgap.beaconsdk.core.internal.message.v2

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.compat.Compat
import it.airgap.beaconsdk.core.internal.compat.VersionedCompat
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.blockchainRegistry
import it.airgap.beaconsdk.core.internal.utils.compat
import it.airgap.beaconsdk.core.internal.utils.getString
import it.airgap.beaconsdk.core.message.AcknowledgeBeaconResponse
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.DisconnectBeaconMessage
import it.airgap.beaconsdk.core.message.ErrorBeaconResponse
import it.airgap.beaconsdk.core.scope.BeaconScope
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
public abstract class V2BeaconMessage : VersionedBeaconMessage() {
    public abstract val id: String
    public abstract val type: String
    public abstract val senderId: String

    public companion object {
        public fun from(senderId: String, message: BeaconMessage, context: Context): V2BeaconMessage =
            with(message) {
                when (this) {
                    is AcknowledgeBeaconResponse -> AcknowledgeV2BeaconResponse(version, id, senderId)
                    is ErrorBeaconResponse -> ErrorV2BeaconResponse(version, id, senderId, errorType)
                    is DisconnectBeaconMessage -> DisconnectV2BeaconMessage(version, id, senderId)
                    else -> context.compat.versioned.blockchain.creator.v2.from(senderId, message).getOrThrow()
                }
            }

        public fun serializer(blockchainRegistry: BlockchainRegistry, compat: Compat<VersionedCompat>): KSerializer<V2BeaconMessage> =
            Serializer(blockchainRegistry, compat)

        public fun serializer(beaconScope: BeaconScope? = null): KSerializer<V2BeaconMessage> =
            Serializer(beaconScope)

        public fun compatSerializer(compat: Compat<VersionedCompat>): KSerializer<V2BeaconMessage> = compat.versioned.blockchain.serializer.v2.message
    }

    public class Context(public val compat: Compat<VersionedCompat>)

    @OptIn(ExperimentalSerializationApi::class)
    internal class Serializer(private val blockchainRegistry: BlockchainRegistry, private val compat: Compat<VersionedCompat>) : KJsonSerializer<V2BeaconMessage> {
        constructor(beaconScope: BeaconScope? = null) : this(blockchainRegistry(beaconScope), compat(beaconScope))

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V2BeaconMessage") {
            element<String>("type")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V2BeaconMessage {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))

            return when (type) {
                AcknowledgeV2BeaconResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(AcknowledgeV2BeaconResponse.serializer(), jsonElement)
                ErrorV2BeaconResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(ErrorV2BeaconResponse.serializer(blockchainRegistry, compat), jsonElement)
                DisconnectV2BeaconMessage.TYPE -> jsonDecoder.json.decodeFromJsonElement(DisconnectV2BeaconMessage.serializer(), jsonElement)
                else -> jsonDecoder.json.decodeFromJsonElement(compatSerializer(compat), jsonElement)
            }
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V2BeaconMessage) {
            when (value) {
                is AcknowledgeV2BeaconResponse -> jsonEncoder.encodeSerializableValue(AcknowledgeV2BeaconResponse.serializer(), value)
                is ErrorV2BeaconResponse -> jsonEncoder.encodeSerializableValue(ErrorV2BeaconResponse.serializer(blockchainRegistry, compat), value)
                is DisconnectV2BeaconMessage -> jsonEncoder.encodeSerializableValue(DisconnectV2BeaconMessage.serializer(), value)
                else -> jsonEncoder.encodeSerializableValue(compatSerializer(compat), value)
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

    override suspend fun toBeaconMessage(origin: Origin, destination: Origin, beaconScope: BeaconScope): BeaconMessage =
        AcknowledgeBeaconResponse(id, version, destination, senderId)

    public companion object {
        public const val TYPE: String = "acknowledge"
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class ErrorV2BeaconResponse(
    override val version: String,
    override val id: String,
    override var senderId: String,
    val errorType: BeaconError,
) : V2BeaconMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, destination: Origin, beaconScope: BeaconScope): BeaconMessage =
        ErrorBeaconResponse(id, version, destination, errorType, null)

    public companion object {
        public const val TYPE: String = "error"

        public fun serializer(blockchainRegistry: BlockchainRegistry, compat: Compat<VersionedCompat>): KSerializer<ErrorV2BeaconResponse> =
            Serializer(blockchainRegistry, compat)

        public fun serializer(beaconScope: BeaconScope? = null): KSerializer<ErrorV2BeaconResponse> =
            Serializer(beaconScope)
    }

    internal class Serializer(blockchainRegistry: BlockchainRegistry, compat: Compat<VersionedCompat>) : KJsonSerializer<ErrorV2BeaconResponse> {
        constructor(beaconScope: BeaconScope? = null) : this(blockchainRegistry(beaconScope), compat(beaconScope))

        private val beaconErrorSerializer: KSerializer<BeaconError> = BeaconError.serializer(blockchainRegistry, compat.versioned.blockchain.identifier)

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ErrorV2BeaconResponse") {
            element<String>("type")
            element<String>("version")
            element<String>("id")
            element<String>("senderId")
            element("errorType", beaconErrorSerializer.descriptor)
        }

        override fun deserialize(
            jsonDecoder: JsonDecoder,
            jsonElement: JsonElement,
        ): ErrorV2BeaconResponse = jsonDecoder.decodeStructure(descriptor) {
            val version = decodeStringElement(descriptor, 1)
            val id = decodeStringElement(descriptor, 2)
            val senderId = decodeStringElement(descriptor, 3)
            val errorType = decodeSerializableElement(descriptor, 4, beaconErrorSerializer)

            return ErrorV2BeaconResponse(version, id, senderId, errorType)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: ErrorV2BeaconResponse) {
            jsonEncoder.encodeStructure(descriptor) {
                with(value) {
                    encodeStringElement(descriptor, 0, type)
                    encodeStringElement(descriptor, 1, version)
                    encodeStringElement(descriptor, 2, id)
                    encodeStringElement(descriptor, 3, senderId)
                    encodeSerializableElement(descriptor, 4, beaconErrorSerializer, errorType)
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

    override suspend fun toBeaconMessage(origin: Origin, destination: Origin, beaconScope: BeaconScope): BeaconMessage =
        DisconnectBeaconMessage(id, senderId, version, origin, destination)

    public companion object {
        public const val TYPE: String = "disconnect"
    }
}