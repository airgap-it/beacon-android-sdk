package it.airgap.beaconsdk.core.internal.blockchain.message

import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.data.MockAppMetadata
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.dependencyRegistry
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalArgument
import it.airgap.beaconsdk.core.internal.utils.getSerializable
import it.airgap.beaconsdk.core.internal.utils.getString
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.scope.BeaconScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable(with = V1MockPermissionBeaconRequest.Serializer::class)
internal data class V1MockPermissionBeaconRequest(
    override val type: String,
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val appMetadata: MockAppMetadata,
    val rest: Map<String, JsonElement>,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Connection.Id, destination: Connection.Id, beaconScope: BeaconScope): BeaconMessage =
        PermissionMockRequest(
            type,
            id,
            version,
            MockBlockchain.IDENTIFIER,
            beaconId,
            origin,
            destination,
            appMetadata,
            rest,
        )

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
                descriptor.getElementName(4) to jsonEncoder.json.encodeToJsonElement(value.appMetadata)
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
    override suspend fun toBeaconMessage(origin: Connection.Id, destination: Connection.Id, beaconScope: BeaconScope): BeaconMessage =
        PermissionMockResponse(
            type,
            id,
            version,
            destination,
            MockBlockchain.IDENTIFIER,
            rest,
        )

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
    override suspend fun toBeaconMessage(origin: Connection.Id, destination: Connection.Id, beaconScope: BeaconScope): BeaconMessage =
        when (mockType) {
            MockBeaconMessageType.Request -> {
                val appMetadata = dependencyRegistry(beaconScope).storageManager.findAppMetadata { it.senderId == beaconId }
                BlockchainMockRequest(
                    type,
                    id,
                    version,
                    MockBlockchain.IDENTIFIER,
                    beaconId,
                    appMetadata,
                    origin,
                    destination,
                    null,
                    rest,
                )
            }
            MockBeaconMessageType.Response -> BlockchainMockResponse(
                type,
                id,
                version,
                destination,
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
