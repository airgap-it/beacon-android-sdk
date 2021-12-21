package it.airgap.beaconsdk.core.internal.blockchain.message

import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.message.v1.V1AppMetadata
import it.airgap.beaconsdk.core.internal.message.v3.*
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.getSerializable
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
    val appMetadata: V3AppMetadata,
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
        "permission_request",
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
            element<V1AppMetadata>("appMetadata")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V3MockPermissionBeaconRequestData {
            val appMetadata = jsonElement.jsonObject.getSerializable(descriptor.getElementName(0), jsonDecoder, V3AppMetadata.serializer())
            val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }

            return V3MockPermissionBeaconRequestData(appMetadata, rest)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V3MockPermissionBeaconRequestData) {
            val fields = mapOf(
                descriptor.getElementName(0) to Json.encodeToJsonElement(value.appMetadata)
            ) + value.rest

            jsonEncoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(fields))
        }
    }
}

@Serializable(with = V3MockPermissionBeaconResponseData.Serializer::class)
internal data class V3MockPermissionBeaconResponseData(
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
            "permission_response",
            id,
            version,
            origin,
            MockBlockchain.IDENTIFIER,
            accountId,
            rest,
        )

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer : KJsonSerializer<V3MockPermissionBeaconResponseData> {
        private val knownFields: Set<String> get() = descriptor.elementNames.toSet()

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V3MockPermissionBeaconResponseData")

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V3MockPermissionBeaconResponseData {
            val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }

            return V3MockPermissionBeaconResponseData(rest)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V3MockPermissionBeaconResponseData) {
            val fields = mapOf<String, JsonElement>() + value.rest

            jsonEncoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(fields))
        }
    }
}

@Serializable(with = V3MockBlockchainBeaconRequestData.Serializer::class)
internal data class V3MockBlockchainBeaconRequestData(
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
            "blockchain_request",
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

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V3MockBlockchainBeaconRequestData")

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V3MockBlockchainBeaconRequestData {
            val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }

            return V3MockBlockchainBeaconRequestData(rest)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V3MockBlockchainBeaconRequestData) {
            val fields = mapOf<String, JsonElement>() + value.rest

            jsonEncoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(fields))
        }
    }
}

@Serializable(with = V3MockBlockchainBeaconResponseData.Serializer::class)
internal data class V3MockBlockchainBeaconResponseData(
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
            "blockchain_response",
            id,
            version,
            origin,
            MockBlockchain.IDENTIFIER,
            rest,
        )

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer : KJsonSerializer<V3MockBlockchainBeaconResponseData> {
        private val knownFields: Set<String> get() = descriptor.elementNames.toSet()

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V3MockBlockchainBeaconResponseData")

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V3MockBlockchainBeaconResponseData {
            val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }

            return V3MockBlockchainBeaconResponseData(rest)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V3MockBlockchainBeaconResponseData) {
            val fields = mapOf<String, JsonElement>() + value.rest

            jsonEncoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(fields))
        }
    }
}