package it.airgap.beaconsdk.core.internal.blockchain.message

import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.data.MockAppMetadata
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
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

@Serializable(with = V2MockPermissionBeaconRequest.Serializer::class)
internal data class V2MockPermissionBeaconRequest(
    override val type: String,
    override val version: String,
    override val id: String,
    override val senderId: String,
    val appMetadata: MockAppMetadata,
    val rest: Map<String, JsonElement>,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Connection.Id, destination: Connection.Id, beaconScope: BeaconScope): BeaconMessage =
        PermissionMockRequest(
            type,
            id,
            version,
            MockBlockchain.IDENTIFIER,
            senderId,
            origin,
            destination,
            appMetadata,
            rest,
        )

    object Serializer : KJsonSerializer<V2MockPermissionBeaconRequest> {
        private val knownFields: Set<String> get() = descriptor.elementNames.toSet()

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V2MockPermissionBeaconRequest") {
            element<String>("type")
            element<String>("version")
            element<String>("id")
            element<String>("senderId")
            element<MockAppMetadata>("appMetadata")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V2MockPermissionBeaconRequest {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))
            val version = jsonElement.jsonObject.getString(descriptor.getElementName(1))
            val id = jsonElement.jsonObject.getString(descriptor.getElementName(2))
            val senderId = jsonElement.jsonObject.getString(descriptor.getElementName(3))
            val appMetadata = jsonElement.jsonObject.getSerializable(descriptor.getElementName(4), jsonDecoder, MockAppMetadata.serializer())
            val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }

            return V2MockPermissionBeaconRequest(type, version, id, senderId, appMetadata, rest)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V2MockPermissionBeaconRequest) {
            val fields = mapOf(
                descriptor.getElementName(0) to JsonPrimitive(value.type),
                descriptor.getElementName(1) to JsonPrimitive(value.version),
                descriptor.getElementName(2) to JsonPrimitive(value.id),
                descriptor.getElementName(3) to JsonPrimitive(value.senderId),
                descriptor.getElementName(4) to jsonEncoder.json.encodeToJsonElement(value.appMetadata)
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
    val rest: Map<String, JsonElement>,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Connection.Id, destination: Connection.Id, beaconScope: BeaconScope): BeaconMessage =
        PermissionMockResponse(
            type,
            id,
            version,
            destination,
            MockBlockchain.IDENTIFIER,
            rest,
        )

    object Serializer : KJsonSerializer<V2MockPermissionBeaconResponse> {
        private val knownFields: Set<String> get() = descriptor.elementNames.toSet()

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V2MockPermissionBeaconResponse") {
            element<String>("type")
            element<String>("version")
            element<String>("id")
            element<String>("senderId")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V2MockPermissionBeaconResponse {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))
            val version = jsonElement.jsonObject.getString(descriptor.getElementName(1))
            val id = jsonElement.jsonObject.getString(descriptor.getElementName(2))
            val senderId = jsonElement.jsonObject.getString(descriptor.getElementName(3))
            val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }

            return V2MockPermissionBeaconResponse(type, version, id, senderId, rest)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V2MockPermissionBeaconResponse) {
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

@Serializable(with = V2MockBlockchainBeaconMessage.Serializer::class)
internal data class V2MockBlockchainBeaconMessage(
    override val type: String,
    override val version: String,
    override val id: String,
    override val senderId: String,
    val rest: Map<String, JsonElement>,
    val mockType: MockBeaconMessageType
) : V2BeaconMessage() {

    override suspend fun toBeaconMessage(origin: Connection.Id, destination: Connection.Id, beaconScope: BeaconScope): BeaconMessage =
        when (mockType) {
            MockBeaconMessageType.Request -> {
                val appMetadata = dependencyRegistry(beaconScope).storageManager.findAppMetadata { it.senderId == senderId }
                BlockchainMockRequest(
                    type,
                    id,
                    version,
                    MockBlockchain.IDENTIFIER,
                    senderId,
                    appMetadata,
                    origin,
                    destination,
                    null,
                    rest,
                )
            }
            MockBeaconMessageType.Response -> BlockchainMockResponse(type,
                id,
                version,
                destination,
                MockBlockchain.IDENTIFIER,
                rest)
        }

    companion object {
        fun response(type: String, version: String, id: String, senderId: String, rest: Map<String, JsonElement> = emptyMap()): V2MockBlockchainBeaconMessage =
            V2MockBlockchainBeaconMessage(type, version, id, senderId, rest, MockBeaconMessageType.Response)

        fun request(type: String, version: String, id: String, senderId: String, rest: Map<String, JsonElement> = emptyMap()): V2MockBlockchainBeaconMessage =
            V2MockBlockchainBeaconMessage(type, version, id, senderId, rest, MockBeaconMessageType.Request)
    }

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