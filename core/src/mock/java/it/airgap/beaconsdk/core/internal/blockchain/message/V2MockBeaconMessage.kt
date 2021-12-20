package it.airgap.beaconsdk.core.internal.blockchain.message

import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.data.Threshold
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.message.v1.V1AppMetadata
import it.airgap.beaconsdk.core.internal.message.v2.V2AppMetadata
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
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

@Serializable(with = V2MockPermissionBeaconRequest.Serializer::class)
internal data class V2MockPermissionBeaconRequest(
    override val type: String,
    override val version: String,
    override val id: String,
    override val senderId: String,
    val appMetadata: V2AppMetadata,
    val rest: Map<String, JsonElement>,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager, identifierCreator: IdentifierCreator): BeaconMessage =
        PermissionMockRequest(
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
    object Serializer : KJsonSerializer<V2MockPermissionBeaconRequest> {
        private val knownFields: Set<String> get() = descriptor.elementNames.toSet()

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V2MockPermissionBeaconRequest") {
            element<String>("type")
            element<String>("version")
            element<String>("id")
            element<String>("senderId")
            element<V1AppMetadata>("appMetadata")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V2MockPermissionBeaconRequest {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))
            val version = jsonElement.jsonObject.getString(descriptor.getElementName(1))
            val id = jsonElement.jsonObject.getString(descriptor.getElementName(2))
            val senderId = jsonElement.jsonObject.getString(descriptor.getElementName(3))
            val appMetadata = jsonElement.jsonObject.getSerializable(descriptor.getElementName(4), jsonDecoder, V2AppMetadata.serializer())
            val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }

            return V2MockPermissionBeaconRequest(type, version, id, senderId, appMetadata, rest)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V2MockPermissionBeaconRequest) {
            val fields = mapOf(
                descriptor.getElementName(0) to JsonPrimitive(value.type),
                descriptor.getElementName(1) to JsonPrimitive(value.version),
                descriptor.getElementName(2) to JsonPrimitive(value.id),
                descriptor.getElementName(3) to JsonPrimitive(value.senderId),
                descriptor.getElementName(4) to Json.encodeToJsonElement(value.appMetadata)
            ) + value.rest

            jsonEncoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(fields))
        }
    }
}

@Serializable(with = V2MockPermissionBeaconResponse.Serializer::class)
internal data class V2MockPermissionBeaconResponse(
    override val type: String,
    override val version: String,
    override val id: String,
    override val senderId: String,
    val accountId: String,
    val threshold: Threshold?,
    val rest: Map<String, JsonElement>,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager, identifierCreator: IdentifierCreator): BeaconMessage =
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
    object Serializer : KJsonSerializer<V2MockPermissionBeaconResponse> {
        private val knownFields: Set<String> get() = descriptor.elementNames.toSet()

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V2MockPermissionBeaconResponse") {
            element<String>("type")
            element<String>("version")
            element<String>("id")
            element<String>("senderId")
            element<String>("accountId")
            element<Threshold>("threshold", isOptional = true)
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V2MockPermissionBeaconResponse {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))
            val version = jsonElement.jsonObject.getString(descriptor.getElementName(1))
            val id = jsonElement.jsonObject.getString(descriptor.getElementName(2))
            val senderId = jsonElement.jsonObject.getString(descriptor.getElementName(3))
            val accountId = jsonElement.jsonObject.getString(descriptor.getElementName(4))
            val threshold = jsonElement.jsonObject.getSerializableOrNull(descriptor.getElementName(5), jsonDecoder, Threshold.serializer())
            val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }

            return V2MockPermissionBeaconResponse(type, version, id, senderId, accountId, threshold, rest)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V2MockPermissionBeaconResponse) {
            val fields = mapOf(
                descriptor.getElementName(0) to JsonPrimitive(value.type),
                descriptor.getElementName(1) to JsonPrimitive(value.version),
                descriptor.getElementName(2) to JsonPrimitive(value.id),
                descriptor.getElementName(3) to JsonPrimitive(value.senderId),
                descriptor.getElementName(4) to JsonPrimitive(value.accountId),
                descriptor.getElementName(5) to Json.encodeToJsonElement(value.threshold)
            ) + value.rest

            jsonEncoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(fields))
        }
    }
}

@Serializable(with = V2MockBlockchainBeaconMessage.Serializer::class)
internal data class V2MockBlockchainBeaconMessage(
    override val type: String,
    override val version: String,
    override val id: String,
    override val senderId: String,
    val rest: Map<String, JsonElement>,
    val mockType: MockBeaconMessageType
) : V2BeaconMessage() {

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager, identifierCreator: IdentifierCreator): BeaconMessage =
        when (mockType) {
            MockBeaconMessageType.Request -> {
                val appMetadata = storageManager.findAppMetadata { it.senderId == senderId }
                BlockchainMockRequest(
                    type,
                    id,
                    version,
                    MockBlockchain.IDENTIFIER,
                    senderId,
                    appMetadata,
                    origin,
                    null,
                    rest,
                )
            }
            MockBeaconMessageType.Response -> BlockchainMockResponse(type,
                id,
                version,
                origin,
                MockBlockchain.IDENTIFIER,
                rest)
        }

    companion object {
        fun response(type: String, version: String, id: String, senderId: String, rest: Map<String, JsonElement> = emptyMap()): V2MockBlockchainBeaconMessage =
            V2MockBlockchainBeaconMessage(type, version, id, senderId, rest, MockBeaconMessageType.Response)

        fun request(type: String, version: String, id: String, senderId: String, rest: Map<String, JsonElement> = emptyMap()): V2MockBlockchainBeaconMessage =
            V2MockBlockchainBeaconMessage(type, version, id, senderId, rest, MockBeaconMessageType.Request)
    }

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer : KJsonSerializer<V2MockBlockchainBeaconMessage> {
        private val knownFields: Set<String> get() = descriptor.elementNames.toSet()

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V2MockBlockchainBeaconResponse") {
            element<String>("type")
            element<String>("version")
            element<String>("id")
            element<String>("senderId")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V2MockBlockchainBeaconMessage {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))
            val version = jsonElement.jsonObject.getString(descriptor.getElementName(1))
            val id = jsonElement.jsonObject.getString(descriptor.getElementName(2))
            val senderId = jsonElement.jsonObject.getString(descriptor.getElementName(3))
            val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }
            val mockType = MockBeaconMessageType.from(type) ?: failWithIllegalArgument()

            return V2MockBlockchainBeaconMessage(type, version, id, senderId, rest, mockType)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V2MockBlockchainBeaconMessage) {
            val fields = mapOf(
                descriptor.getElementName(0) to JsonPrimitive(value.type),
                descriptor.getElementName(1) to JsonPrimitive(value.version),
                descriptor.getElementName(2) to JsonPrimitive(value.id),
                descriptor.getElementName(3) to JsonPrimitive(value.senderId),
            ) + value.rest

            jsonEncoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(fields))
        }
    }
}