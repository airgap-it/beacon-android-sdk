package it.airgap.beaconsdk.core.internal.blockchain.message

import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.data.Threshold
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.message.v1.V1AppMetadata
import it.airgap.beaconsdk.core.internal.message.v2.V2AppMetadata
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v3.*
import it.airgap.beaconsdk.core.internal.message.v3.V3AppMetadata
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.*
import it.airgap.beaconsdk.core.message.BeaconMessage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.*

@Serializable(with = V3MockPermissionBeaconRequestData.Serializer::class)
internal data class V3MockPermissionBeaconRequestData(
    val type: String,
    val appMetadata: V2AppMetadata,
    val rest: Map<String, JsonElement>,
) : PermissionV3BeaconRequestContent.ChainData() {
    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        storageManager: StorageManager,
        identifierCreator: IdentifierCreator,
    ): BeaconMessage = PermissionMockRequest(
        type,
        id,
        version,
        MockBlockchain.IDENTIFIER,
        senderId,
        origin,
        appMetadata.toAppMetadata(),
        rest,
    )

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer : KJsonSerializer<V3MockPermissionBeaconRequestData> {
        private val knownFields: Set<String> get() = descriptor.elementNames.toSet()

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V3MockPermissionBeaconRequestData") {
            element<String>("type")
            element<V1AppMetadata>("appMetadata")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V3MockPermissionBeaconRequestData {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))
            val appMetadata = jsonElement.jsonObject.getSerializable(descriptor.getElementName(1), jsonDecoder, V3AppMetadata.serializer())
            val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }

            return V3MockPermissionBeaconRequestData(type, appMetadata, rest)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V3MockPermissionBeaconRequestData) {
            val fields = mapOf(
                descriptor.getElementName(0) to JsonPrimitive(value.type),
                descriptor.getElementName(1) to Json.encodeToJsonElement(value.appMetadata)
            ) + value.rest

            jsonEncoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(fields))
        }
    }
}

@Serializable(with = V3MockPermissionBeaconResponseData.Serializer::class)
internal data class V3MockPermissionBeaconResponseData(
    val type: String,
    val threshold: Threshold?,
    val rest: Map<String, JsonElement>,
) : PermissionV3BeaconResponseContent.ChainData() {
    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        accountId: String,
        storageManager: StorageManager,
        identifierCreator: IdentifierCreator,
    ): BeaconMessage =
        PermissionMockResponse(
            type,
            id,
            version,
            origin,
            MockBlockchain.IDENTIFIER,
            accountId,
            threshold,
            rest,
        )

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer : KJsonSerializer<V3MockPermissionBeaconResponseData> {
        private val knownFields: Set<String> get() = descriptor.elementNames.toSet()

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V3MockPermissionBeaconResponseData") {
            element<String>("type")
            element<Threshold>("threshold", isOptional = true)
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V3MockPermissionBeaconResponseData {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))
            val threshold = jsonElement.jsonObject.getSerializableOrNull(descriptor.getElementName(1), jsonDecoder, Threshold.serializer())
            val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }

            return V3MockPermissionBeaconResponseData(type, threshold, rest)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V3MockPermissionBeaconResponseData) {
            val fields = mapOf(
                descriptor.getElementName(0) to JsonPrimitive(value.type),
                descriptor.getElementName(1) to Json.encodeToJsonElement(value.threshold)
            ) + value.rest

            jsonEncoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(fields))
        }
    }
}

@Serializable(with = V3MockBlockchainBeaconRequestData.Serializer::class)
internal data class V3MockBlockchainBeaconRequestData(
    val type: String,
    val rest: Map<String, JsonElement>
) : BlockchainV3BeaconRequestContent.ChainData() {
    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        accountId: String,
        storageManager: StorageManager,
        identifierCreator: IdentifierCreator,
    ): BeaconMessage {
        val appMetadata = storageManager.findAppMetadata { it.senderId == senderId }
        return BlockchainMockRequest(
            type,
            id,
            version,
            MockBlockchain.IDENTIFIER,
            senderId,
            appMetadata,
            origin,
            accountId,
            rest,
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer : KJsonSerializer<V3MockBlockchainBeaconRequestData> {
        private val knownFields: Set<String> get() = descriptor.elementNames.toSet()

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V3MockBlockchainBeaconRequestData") {
            element<String>("type")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V3MockBlockchainBeaconRequestData {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))
            val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }

            return V3MockBlockchainBeaconRequestData(type, rest)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V3MockBlockchainBeaconRequestData) {
            val fields = mapOf(
                descriptor.getElementName(0) to JsonPrimitive(value.type),
            ) + value.rest

            jsonEncoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(fields))
        }
    }
}

@Serializable(with = V3MockBlockchainBeaconResponseData.Serializer::class)
internal data class V3MockBlockchainBeaconResponseData(
    val type: String,
    val rest: Map<String, JsonElement>
) : BlockchainV3BeaconResponseContent.ChainData() {
    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        storageManager: StorageManager,
        identifierCreator: IdentifierCreator,
    ): BeaconMessage =
        BlockchainMockResponse(
            type,
            id,
            version,
            origin,
            MockBlockchain.IDENTIFIER,
            rest,
        )

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer : KJsonSerializer<V3MockBlockchainBeaconResponseData> {
        private val knownFields: Set<String> get() = descriptor.elementNames.toSet()

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V3MockBlockchainBeaconResponseData") {
            element<String>("type")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V3MockBlockchainBeaconResponseData {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))
            val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }

            return V3MockBlockchainBeaconResponseData(type, rest)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V3MockBlockchainBeaconResponseData) {
            val fields = mapOf(
                descriptor.getElementName(0) to JsonPrimitive(value.type),
            ) + value.rest

            jsonEncoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(fields))
        }
    }
}