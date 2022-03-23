package it.airgap.beaconsdk.core.internal.blockchain.message

import it.airgap.beaconsdk.core.data.MockAppMetadata
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.utils.*
import it.airgap.beaconsdk.core.message.BeaconMessage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.*

@Serializable(with = V1MockPermissionBeaconRequest.Serializer::class)
internal data class V1MockPermissionBeaconRequest(
    override val type: String,
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val appMetadata: MockAppMetadata,
    val rest: Map<String, JsonElement>,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin): BeaconMessage =
        PermissionMockRequest(
            type,
            id,
            version,
            MockBlockchain.IDENTIFIER,
            beaconId,
            origin,
            appMetadata,
            rest,
        )

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer : KJsonSerializer<V1MockPermissionBeaconRequest> {
        private val knownFields: Set<String> get() = descriptor.elementNames.toSet()

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V1MockPermissionBeaconRequest") {
            element<String>("type")
            element<String>("version")
            element<String>("id")
            element<String>("beaconId")
            element<MockAppMetadata>("appMetadata")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V1MockPermissionBeaconRequest {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))
            val version = jsonElement.jsonObject.getString(descriptor.getElementName(1))
            val id = jsonElement.jsonObject.getString(descriptor.getElementName(2))
            val beaconId = jsonElement.jsonObject.getString(descriptor.getElementName(3))
            val appMetadata = jsonElement.jsonObject.getSerializable(descriptor.getElementName(4), jsonDecoder, MockAppMetadata.serializer())
            val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }

            return V1MockPermissionBeaconRequest(type, version, id, beaconId, appMetadata, rest)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V1MockPermissionBeaconRequest) {
            val fields = mapOf(
                descriptor.getElementName(0) to JsonPrimitive(value.type),
                descriptor.getElementName(1) to JsonPrimitive(value.version),
                descriptor.getElementName(2) to JsonPrimitive(value.id),
                descriptor.getElementName(3) to JsonPrimitive(value.beaconId),
                descriptor.getElementName(4) to Json.encodeToJsonElement(value.appMetadata)
            ) + value.rest

            jsonEncoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(fields))
        }
    }
}

@Serializable(with = V1MockPermissionBeaconResponse.Serializer::class)
internal data class V1MockPermissionBeaconResponse(
    override val type: String,
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val rest: Map<String, JsonElement>,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin): BeaconMessage =
        PermissionMockResponse(
            type,
            id,
            version,
            origin,
            MockBlockchain.IDENTIFIER,
            rest,
        )

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer : KJsonSerializer<V1MockPermissionBeaconResponse> {
        private val knownFields: Set<String> get() = descriptor.elementNames.toSet()

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V1MockPermissionBeaconResponse") {
            element<String>("type")
            element<String>("version")
            element<String>("id")
            element<String>("beaconId")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V1MockPermissionBeaconResponse {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))
            val version = jsonElement.jsonObject.getString(descriptor.getElementName(1))
            val id = jsonElement.jsonObject.getString(descriptor.getElementName(2))
            val beaconId = jsonElement.jsonObject.getString(descriptor.getElementName(3))
            val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }

            return V1MockPermissionBeaconResponse(type, version, id, beaconId, rest)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V1MockPermissionBeaconResponse) {
            val fields = mapOf(
                descriptor.getElementName(0) to JsonPrimitive(value.type),
                descriptor.getElementName(1) to JsonPrimitive(value.version),
                descriptor.getElementName(2) to JsonPrimitive(value.id),
                descriptor.getElementName(3) to JsonPrimitive(value.beaconId),
            ) + value.rest

            jsonEncoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(fields))
        }
    }
}

@Serializable(with = V1MockBlockchainBeaconMessage.Serializer::class)
internal data class V1MockBlockchainBeaconMessage(
    override val type: String,
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val rest: Map<String, JsonElement>,
    val mockType: MockBeaconMessageType,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin): BeaconMessage =
        when (mockType) {
            MockBeaconMessageType.Request -> {
                val appMetadata = dependencyRegistry.storageManager.findAppMetadata { it.senderId == beaconId }
                BlockchainMockRequest(
                    type,
                    id,
                    version,
                    MockBlockchain.IDENTIFIER,
                    beaconId,
                    appMetadata,
                    origin,
                    null,
                    rest,
                )
            }
            MockBeaconMessageType.Response -> BlockchainMockResponse(
                type,
                id,
                version,
                origin,
                MockBlockchain.IDENTIFIER,
                rest,
            )
        }

    companion object {
        fun response(type: String, version: String, id: String, beaconId: String, rest: Map<String, JsonElement> = emptyMap()): V1MockBlockchainBeaconMessage =
            V1MockBlockchainBeaconMessage(type, version, id, beaconId, rest, MockBeaconMessageType.Response)

        fun request(type: String, version: String, id: String, beaconId: String, rest: Map<String, JsonElement> = emptyMap()): V1MockBlockchainBeaconMessage =
            V1MockBlockchainBeaconMessage(type, version, id, beaconId, rest, MockBeaconMessageType.Request)
    }

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer : KJsonSerializer<V1MockBlockchainBeaconMessage> {
        private val knownFields: Set<String> get() = descriptor.elementNames.toSet()

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V1MockBlockchainBeaconResponse") {
            element<String>("type")
            element<String>("version")
            element<String>("id")
            element<String>("beaconId")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V1MockBlockchainBeaconMessage {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))
            val version = jsonElement.jsonObject.getString(descriptor.getElementName(1))
            val id = jsonElement.jsonObject.getString(descriptor.getElementName(2))
            val beaconId = jsonElement.jsonObject.getString(descriptor.getElementName(3))
            val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }
            val mockType = MockBeaconMessageType.from(type) ?: failWithIllegalArgument()

            return V1MockBlockchainBeaconMessage(type, version, id, beaconId, rest, mockType)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V1MockBlockchainBeaconMessage) {
            val fields = mapOf(
                descriptor.getElementName(0) to JsonPrimitive(value.type),
                descriptor.getElementName(1) to JsonPrimitive(value.version),
                descriptor.getElementName(2) to JsonPrimitive(value.id),
                descriptor.getElementName(3) to JsonPrimitive(value.beaconId),
            ) + value.rest

            jsonEncoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(fields))
        }
    }
}
