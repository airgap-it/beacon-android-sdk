package it.airgap.beaconsdk.core.internal.message.v1

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.*
import it.airgap.beaconsdk.core.internal.chain.Chain
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.*
import it.airgap.beaconsdk.core.message.*
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

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Serializable(with = V1BeaconMessage.Serializer::class)
public abstract class V1BeaconMessage : VersionedBeaconMessage() {
    public abstract val id: String
    public abstract val beaconId: String

    public companion object : Factory<BeaconMessage, V1BeaconMessage> {
        override fun from(senderId: String, content: BeaconMessage): V1BeaconMessage =
            with(content) {
                when (this) {
                    is PermissionBeaconRequest -> PermissionV1BeaconRequest(version, id, this.senderId, V1AppMetadata.fromAppMetadata(appMetadata), network, scopes)
                    is PermissionBeaconResponse -> PermissionV1BeaconResponse(version, id, senderId, publicKey, network, scopes, threshold)
                    is AcknowledgeBeaconResponse -> failWithUnsupportedMessage(content, version)
                    is ErrorBeaconResponse -> ErrorV1BeaconResponse(version, id, senderId, errorType)
                    is DisconnectBeaconMessage -> DisconnectV1BeaconMessage(version, id, senderId)
                    else -> CompatFactory.from(senderId, content)
                }
            }

        public fun compatSerializer(): KSerializer<V1BeaconMessage> = CompatFactory.serializer()
    }

    internal object Serializer : KJsonSerializer<V1BeaconMessage> {
        private object Field {
            const val TYPE = "type"
        }

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V1BeaconMessage") {
            element<String>(Field.TYPE)
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V1BeaconMessage {
            val type = jsonElement.jsonObject.getString(Field.TYPE)

            return when (type) {
                PermissionV1BeaconRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(PermissionV1BeaconRequest.serializer(), jsonElement)
                PermissionV1BeaconResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(PermissionV1BeaconResponse.serializer(), jsonElement)
                ErrorV1BeaconResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(ErrorV1BeaconResponse.serializer(), jsonElement)
                DisconnectV1BeaconMessage.TYPE -> jsonDecoder.json.decodeFromJsonElement(DisconnectV1BeaconMessage.serializer(), jsonElement)
                else -> jsonDecoder.json.decodeFromJsonElement(compatSerializer(), jsonElement)
            }
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V1BeaconMessage) {
            when (value) {
                is PermissionV1BeaconRequest -> jsonEncoder.encodeSerializableValue(PermissionV1BeaconRequest.serializer(), value)
                is PermissionV1BeaconResponse -> jsonEncoder.encodeSerializableValue(PermissionV1BeaconResponse.serializer(), value)
                is ErrorV1BeaconResponse -> jsonEncoder.encodeSerializableValue(ErrorV1BeaconResponse.serializer(), value)
                is DisconnectV1BeaconMessage -> jsonEncoder.encodeSerializableValue(DisconnectV1BeaconMessage.serializer(), value)
                else -> jsonEncoder.encodeSerializableValue(compatSerializer(), value)
            }
        }
    }

    internal object CompatFactory : Factory<BeaconMessage, V1BeaconMessage> {
        private const val CHAIN_IDENTIFIER = "tezos"
        val chain: Chain<*, *>
            get() = chainRegistry.get(CHAIN_IDENTIFIER) ?: failWithChainNotFound(CHAIN_IDENTIFIER)

        override fun from(senderId: String, content: BeaconMessage): V1BeaconMessage = chain.serializer.v1.from(senderId, content)
        override fun serializer(): KSerializer<V1BeaconMessage> = chain.serializer.v1.serializer()
    }
}

@Serializable
public data class PermissionV1BeaconRequest(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val appMetadata: V1AppMetadata,
    val network: Network,
    val scopes: List<Permission.Scope>,
) : V1BeaconMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        PermissionBeaconRequest(id, beaconId, appMetadata.toAppMetadata(), CompatFactory.chain.identifier, network, scopes, origin, version)

    public companion object {
        public const val TYPE: String = "permission_request"
    }
}

@Serializable
public data class PermissionV1BeaconResponse(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val publicKey: String,
    val network: Network,
    val scopes: List<Permission.Scope>,
    val threshold: Threshold? = null,
) : V1BeaconMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        PermissionBeaconResponse(id, publicKey, CompatFactory.chain.identifier, network, scopes, threshold, version, origin)

    public companion object {
        public const val TYPE: String = "permission_response"
    }
}

@Serializable(with = ErrorV1BeaconResponse.Serializer::class)
public data class ErrorV1BeaconResponse(
    override val version: String,
    override val id: String,
    override var beaconId: String,
    val errorType: BeaconError,
) : V1BeaconMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        ErrorBeaconResponse(id, CompatFactory.chain.identifier, errorType, version, origin)

    public companion object {
        public const val TYPE: String = "error"
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
            val errorType = decodeSerializableElement(descriptor, 4, BeaconError.serializer(CompatFactory.chain.identifier))

            return ErrorV1BeaconResponse(version, id, beaconId, errorType)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: ErrorV1BeaconResponse) {
            jsonEncoder.encodeStructure(descriptor) {
                with(value) {
                    encodeStringElement(descriptor, 0, type)
                    encodeStringElement(descriptor, 1, version)
                    encodeStringElement(descriptor, 2, id)
                    encodeStringElement(descriptor, 3, beaconId)
                    encodeSerializableElement(descriptor, 4, BeaconError.serializer(CompatFactory.chain.identifier), errorType)
                }
            }
        }
    }
}

@Serializable
public data class DisconnectV1BeaconMessage(
    override val version: String,
    override val id: String,
    override var beaconId: String,
) : V1BeaconMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        DisconnectBeaconMessage(id, beaconId, version, origin)

    public companion object {
        public const val TYPE: String = "disconnect"
    }
}