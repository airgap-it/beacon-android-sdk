package it.airgap.beaconsdk.core.internal.blockchain.message

import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.data.MockAppMetadata
import it.airgap.beaconsdk.core.internal.message.v3.BlockchainV3BeaconRequestContent
import it.airgap.beaconsdk.core.internal.message.v3.BlockchainV3BeaconResponseContent
import it.airgap.beaconsdk.core.internal.message.v3.PermissionV3BeaconRequestContent
import it.airgap.beaconsdk.core.internal.message.v3.PermissionV3BeaconResponseContent
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.dependencyRegistry
import it.airgap.beaconsdk.core.internal.utils.getSerializable
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
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable(with = V3MockPermissionBeaconRequestData.Serializer::class)
internal data class V3MockPermissionBeaconRequestData(
    val appMetadata: MockAppMetadata,
    val rest: Map<String, JsonElement>,
) : PermissionV3BeaconRequestContent.BlockchainData() {
    override suspend fun toBeaconMessage(
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
        blockchainIdentifier: String,
    ): BeaconMessage = PermissionMockRequest(
        "permission_request",
        id,
        version,
        blockchainIdentifier,
        senderId,
        origin,
        destination,
        appMetadata,
        rest,
    )

    object Serializer : KJsonSerializer<V3MockPermissionBeaconRequestData> {
        private val knownFields: Set<String> get() = descriptor.elementNames.toSet()

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V3MockPermissionBeaconRequestData") {
            element<MockAppMetadata>("appMetadata")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V3MockPermissionBeaconRequestData {
            val appMetadata = jsonElement.jsonObject.getSerializable(descriptor.getElementName(0), jsonDecoder, MockAppMetadata.serializer())
            val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }

            return V3MockPermissionBeaconRequestData(appMetadata, rest)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V3MockPermissionBeaconRequestData) {
            val fields = mapOf(
                descriptor.getElementName(0) to jsonEncoder.json.encodeToJsonElement(value.appMetadata)
            ) + value.rest

            jsonEncoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(fields))
        }
    }
}

@Serializable(with = V3MockPermissionBeaconResponseData.Serializer::class)
internal data class V3MockPermissionBeaconResponseData(
    val rest: Map<String, JsonElement>,
) : PermissionV3BeaconResponseContent.BlockchainData() {
    override suspend fun toBeaconMessage(
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
        blockchainIdentifier: String,
    ): BeaconMessage =
        PermissionMockResponse(
            "permission_response",
            id,
            version,
            destination,
            blockchainIdentifier,
            rest,
        )

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
) : BlockchainV3BeaconRequestContent.BlockchainData() {
    override suspend fun toBeaconMessage(
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
        accountId: String,
        blockchainIdentifier: String,
    ): BeaconMessage {
        val appMetadata = dependencyRegistry(beaconScope).storageManager.findAppMetadata { it.senderId == senderId }
        return BlockchainMockRequest(
            "blockchain_request",
            id,
            version,
            blockchainIdentifier,
            senderId,
            appMetadata,
            origin,
            destination,
            accountId,
            rest,
        )
    }

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
) : BlockchainV3BeaconResponseContent.BlockchainData() {
    override suspend fun toBeaconMessage(
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
        blockchainIdentifier: String,
    ): BeaconMessage =
        BlockchainMockResponse(
            "blockchain_response",
            id,
            version,
            destination,
            blockchainIdentifier,
            rest,
        )

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