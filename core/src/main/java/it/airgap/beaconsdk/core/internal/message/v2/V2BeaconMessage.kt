package it.airgap.beaconsdk.core.internal.message.v2

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.*
import it.airgap.beaconsdk.core.internal.chain.Chain
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.chainRegistry
import it.airgap.beaconsdk.core.internal.utils.failWithChainNotFound
import it.airgap.beaconsdk.core.internal.utils.getString
import it.airgap.beaconsdk.core.message.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.jsonObject

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Serializable(with = V2BeaconMessage.Serializer::class)
public abstract class V2BeaconMessage : VersionedBeaconMessage() {
    public abstract val id: String
    public abstract val senderId: String

    public companion object : Factory<BeaconMessage, V2BeaconMessage> {
        override fun from(senderId: String, content: BeaconMessage): V2BeaconMessage =
            with(content) {
                when (this) {
                    is PermissionBeaconRequest -> PermissionV2BeaconRequest(version, id, senderId, V2AppMetadata.fromAppMetadata(appMetadata), network, scopes)
                    is PermissionBeaconResponse -> PermissionV2BeaconResponse(version, id, senderId, publicKey, network, scopes, threshold)
                    is AcknowledgeBeaconResponse -> AcknowledgeV2BeaconResponse(version, id, senderId)
                    is ErrorBeaconResponse -> ErrorV2BeaconResponse(version, id, senderId, errorType)
                    is DisconnectBeaconMessage -> DisconnectV2BeaconMessage(version, id, senderId)
                    else -> CompatFactory.from(senderId, content)
                }
            }

        public fun compatSerializer(): KSerializer<V2BeaconMessage> = CompatFactory.serializer()
    }

    internal object Serializer : KJsonSerializer<V2BeaconMessage>() {
        private object Field {
            const val TYPE = "type"
        }

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V2BeaconMessage") {
            element<String>(Field.TYPE)
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V2BeaconMessage {
            val type = jsonElement.jsonObject.getString(Field.TYPE)

            return when (type) {
                PermissionV2BeaconRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(PermissionV2BeaconRequest.serializer(), jsonElement)
                PermissionV2BeaconResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(PermissionV2BeaconResponse.serializer(), jsonElement)
                AcknowledgeV2BeaconResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(AcknowledgeV2BeaconResponse.serializer(), jsonElement)
                ErrorV2BeaconResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(ErrorV2BeaconResponse.serializer(), jsonElement)
                DisconnectV2BeaconMessage.TYPE -> jsonDecoder.json.decodeFromJsonElement(DisconnectV2BeaconMessage.serializer(), jsonElement)
                else -> jsonDecoder.json.decodeFromJsonElement(compatSerializer(), jsonElement)
            }
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V2BeaconMessage) {
            when (value) {
                is PermissionV2BeaconRequest -> jsonEncoder.encodeSerializableValue(PermissionV2BeaconRequest.serializer(), value)
                is PermissionV2BeaconResponse -> jsonEncoder.encodeSerializableValue(PermissionV2BeaconResponse.serializer(), value)
                is AcknowledgeV2BeaconResponse -> jsonEncoder.encodeSerializableValue(AcknowledgeV2BeaconResponse.serializer(), value)
                is ErrorV2BeaconResponse -> jsonEncoder.encodeSerializableValue(ErrorV2BeaconResponse.serializer(), value)
                is DisconnectV2BeaconMessage -> jsonEncoder.encodeSerializableValue(DisconnectV2BeaconMessage.serializer(), value)
                else -> jsonEncoder.encodeSerializableValue(compatSerializer(), value)
            }
        }
    }

    internal object CompatFactory : Factory<BeaconMessage, V2BeaconMessage> {
        private const val CHAIN_IDENTIFIER = "tezos"
        val chain: Chain<*, *>
            get() = chainRegistry.get(CHAIN_IDENTIFIER) ?: failWithChainNotFound(CHAIN_IDENTIFIER)

        override fun from(senderId: String, content: BeaconMessage): V2BeaconMessage = chain.serializer.v2.from(senderId, content)
        override fun serializer(): KSerializer<V2BeaconMessage> = chain.serializer.v2.serializer()
    }
}

@Serializable
@SerialName("permission_request")
public data class PermissionV2BeaconRequest(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val appMetadata: V2AppMetadata,
    val network: Network,
    val scopes: List<Permission.Scope>,
) : V2BeaconMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        PermissionBeaconRequest(id, senderId, appMetadata.toAppMetadata(), CompatFactory.chain.identifier, network, scopes, origin, version)

    public companion object {
        public const val TYPE: String = "permission_request"
    }
}

@Serializable
@SerialName("permission_response")
public data class PermissionV2BeaconResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val publicKey: String,
    val network: Network,
    val scopes: List<Permission.Scope>,
    val threshold: Threshold? = null,
) : V2BeaconMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        PermissionBeaconResponse(id, publicKey, CompatFactory.chain.identifier, network, scopes, threshold, version, origin)

    public companion object {
        public const val TYPE: String = "permission_response"
    }
}

@Serializable
@SerialName("acknowledge")
public data class AcknowledgeV2BeaconResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
) : V2BeaconMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        AcknowledgeBeaconResponse(id, senderId, version, origin)

    public companion object {
        public const val TYPE: String = "acknowledge"
    }
}

@Serializable
@SerialName("error")
public data class ErrorV2BeaconResponse(
    override val version: String,
    override val id: String,
    override var senderId: String,
    val errorType: BeaconError,
) : V2BeaconMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        ErrorBeaconResponse(id, CompatFactory.chain.identifier, errorType, version, origin)

    public companion object {
        public const val TYPE: String = "error"
    }
}

@Serializable
@SerialName("disconnect")
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