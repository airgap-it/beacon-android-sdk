package it.airgap.beaconsdk.core.internal.blockchain

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.data.*
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.message.v1.V1AppMetadata
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v2.V2AppMetadata
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.*
import it.airgap.beaconsdk.core.message.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MockBlockchain : Blockchain {
    override val identifier: String = IDENTIFIER
    override val wallet: Blockchain.Wallet = MockBlockchainWallet()
    override val creator: Blockchain.Creator = MockBlockhainCreator()
    override val serializer: Blockchain.Serializer = MockBlockchainSerializer()

    public class Factory : Blockchain.Factory<MockBlockchain> {
        override val identifier: String = IDENTIFIER
        override fun create(dependencyRegistry: DependencyRegistry): MockBlockchain = MockBlockchain()
    }

    public companion object {
        public const val IDENTIFIER: String = "mockBlockchain"
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MockBlockchainWallet : Blockchain.Wallet {
    override fun addressFromPublicKey(publicKey: String): Result<String> =
        Result.success("@$publicKey")
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MockBlockhainCreator : Blockchain.Creator {
    override suspend fun extractPermission(
        request: PermissionBeaconRequest,
        response: PermissionBeaconResponse,
    ): Result<Permission> = runCatching {
        val address = "@${response.publicKey}"

        MockBlockchainSerializer.MockPermission(
            response.blockchainIdentifier,
            address,
            address,
            request.senderId,
            request.appMetadata,
            response.publicKey,
            currentTimestamp(),
            null,
            if (response is MockBlockchainSerializer.PermissionMockResponse) response.rest else emptyMap()
        )
    }

    override fun v1From(senderId: String, content: BeaconMessage): Result<V1BeaconMessage> =
        runCatching {
            when (content) {
                is MockBlockchainSerializer.PermissionMockRequest -> content.toV1()
                is MockBlockchainSerializer.BlockchainMockRequest -> content.toV1()
                is MockBlockchainSerializer.PermissionMockResponse -> content.toV1(senderId)
                is MockBlockchainSerializer.BlockchainMockResponse -> content.toV1(senderId)
                else -> failWithUnsupportedMessage(content, content.version)
            }
        }

    override fun v2From(senderId: String, content: BeaconMessage): Result<V2BeaconMessage> =
        runCatching {
            when (content) {
                is MockBlockchainSerializer.PermissionMockRequest -> content.toV2()
                is MockBlockchainSerializer.BlockchainMockRequest -> content.toV2()
                is MockBlockchainSerializer.PermissionMockResponse -> content.toV2(senderId)
                is MockBlockchainSerializer.BlockchainMockResponse -> content.toV2(senderId)
                else -> failWithUnsupportedMessage(content, content.version)
            }
        }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MockBlockchainSerializer : Blockchain.Serializer {
    override val network: KSerializer<Network>
        get() = SuperClassSerializer(MockNetwork.serializer())

    override val permission: KSerializer<Permission>
        get() = SuperClassSerializer(MockPermission.serializer())

    override val error: KSerializer<BeaconError>
        get() = SuperClassSerializer(MockError.serializer())

    @OptIn(ExperimentalSerializationApi::class)
    override val v1: KSerializer<V1BeaconMessage> =
        object : KJsonSerializer<V1BeaconMessage> {
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MockV1BeaconMessage") {
                element<String>("type")
            }

            override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V1BeaconMessage {
                val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))

                return when (type) {
                    "permission_request" -> jsonDecoder.json.decodeFromJsonElement(V1MockPermissionBeaconRequest.serializer(), jsonElement)
                    "permission_response" -> jsonDecoder.json.decodeFromJsonElement(V1MockPermissionBeaconResponse.serializer(), jsonElement)
                    else -> jsonDecoder.json.decodeFromJsonElement(V1MockBlockchainBeaconMessage.serializer(), jsonElement)
                }
            }

            override fun serialize(jsonEncoder: JsonEncoder, value: V1BeaconMessage) {
                when (value) {
                    is V1MockPermissionBeaconRequest -> jsonEncoder.encodeSerializableValue(V1MockPermissionBeaconRequest.serializer(), value)
                    is V1MockPermissionBeaconResponse -> jsonEncoder.encodeSerializableValue(V1MockPermissionBeaconResponse.serializer(), value)
                    is V1MockBlockchainBeaconMessage -> jsonEncoder.encodeSerializableValue(V1MockBlockchainBeaconMessage.serializer(), value)
                    else -> failWithIllegalArgument()
                }
            }
        }

    @OptIn(ExperimentalSerializationApi::class)
    override val v2: KSerializer<V2BeaconMessage> =
            object : KJsonSerializer<V2BeaconMessage> {
                override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MockV2BeaconMessage") {
                    element<String>("type")
                }

                override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V2BeaconMessage {
                    val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))

                    return when (type) {
                        "permission_request" -> jsonDecoder.json.decodeFromJsonElement(V2MockPermissionBeaconRequest.serializer(), jsonElement)
                        "permission_response" -> jsonDecoder.json.decodeFromJsonElement(V2MockPermissionBeaconResponse.serializer(), jsonElement)
                        else -> jsonDecoder.json.decodeFromJsonElement(V2MockBlockchainBeaconMessage.serializer(), jsonElement)
                    }
                }

                override fun serialize(jsonEncoder: JsonEncoder, value: V2BeaconMessage) {
                    when (value) {
                        is V2MockPermissionBeaconRequest -> jsonEncoder.encodeSerializableValue(V2MockPermissionBeaconRequest.serializer(), value)
                        is V2MockPermissionBeaconResponse -> jsonEncoder.encodeSerializableValue(V2MockPermissionBeaconResponse.serializer(), value)
                        is V2MockBlockchainBeaconMessage -> jsonEncoder.encodeSerializableValue(V2MockBlockchainBeaconMessage.serializer(), value)
                        else -> failWithIllegalArgument()
                    }
                }
        }

    internal enum class MockMessageType(val value: String) {
        Request("request"),
        Response("response");

        companion object {
            fun from(string: String): MockMessageType? = values().firstOrNull { string.contains(it.value) }
        }
    }

    @Serializable(with = V1MockPermissionBeaconRequest.Serializer::class)
    internal data class V1MockPermissionBeaconRequest(
        override val type: String,
        override val version: String,
        override val id: String,
        override val beaconId: String,
        val appMetadata: V1AppMetadata,
        val rest: Map<String, JsonElement>,
    ) : V1BeaconMessage() {
        override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
            PermissionMockRequest(
                type,
                id,
                version,
                MockBlockchain.IDENTIFIER,
                beaconId,
                origin,
                appMetadata.toAppMetadata(),
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
                element<V1AppMetadata>("appMetadata")
            }

            override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V1MockPermissionBeaconRequest {
                val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))
                val version = jsonElement.jsonObject.getString(descriptor.getElementName(1))
                val id = jsonElement.jsonObject.getString(descriptor.getElementName(2))
                val beaconId = jsonElement.jsonObject.getString(descriptor.getElementName(3))
                val appMetadata = jsonElement.jsonObject.getSerializable(descriptor.getElementName(4), jsonDecoder, V1AppMetadata.serializer())
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
        val publicKey: String,
        val threshold: Threshold?,
        val rest: Map<String, JsonElement>,
    ) : V1BeaconMessage() {
        override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
            PermissionMockResponse(
                type,
                id,
                version,
                origin,
                MockBlockchain.IDENTIFIER,
                publicKey,
                threshold,
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
                element<String>("publicKey")
                element<Threshold>("threshold", isOptional = true)
            }

            override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V1MockPermissionBeaconResponse {
                val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))
                val version = jsonElement.jsonObject.getString(descriptor.getElementName(1))
                val id = jsonElement.jsonObject.getString(descriptor.getElementName(2))
                val beaconId = jsonElement.jsonObject.getString(descriptor.getElementName(3))
                val publicKey = jsonElement.jsonObject.getString(descriptor.getElementName(4))
                val threshold = jsonElement.jsonObject.getSerializableOrNull(descriptor.getElementName(5), jsonDecoder, Threshold.serializer())
                val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }

                return V1MockPermissionBeaconResponse(type, version, id, beaconId, publicKey, threshold, rest)
            }

            override fun serialize(jsonEncoder: JsonEncoder, value: V1MockPermissionBeaconResponse) {
                val fields = mapOf(
                    descriptor.getElementName(0) to JsonPrimitive(value.type),
                    descriptor.getElementName(1) to JsonPrimitive(value.version),
                    descriptor.getElementName(2) to JsonPrimitive(value.id),
                    descriptor.getElementName(3) to JsonPrimitive(value.beaconId),
                    descriptor.getElementName(4) to JsonPrimitive(value.publicKey),
                    descriptor.getElementName(5) to Json.encodeToJsonElement(value.threshold)
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
        val mockType: MockMessageType,
    ) : V1BeaconMessage() {
        override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
            when (mockType) {
                MockMessageType.Request -> {
                    val appMetadata = storageManager.findAppMetadata { it.senderId == beaconId }
                    BlockchainMockRequest(
                        type,
                        id,
                        version,
                        MockBlockchain.IDENTIFIER,
                        beaconId,
                        appMetadata,
                        origin,
                        rest,
                    )
                }
                MockMessageType.Response -> BlockchainMockResponse(type, id, version, origin, MockBlockchain.IDENTIFIER, rest)
            }

        companion object {
            fun response(type: String, version: String, id: String, beaconId: String, rest: Map<String, JsonElement> = emptyMap()): V1MockBlockchainBeaconMessage =
                V1MockBlockchainBeaconMessage(type, version, id, beaconId, rest, MockMessageType.Response)

            fun request(type: String, version: String, id: String, beaconId: String, rest: Map<String, JsonElement> = emptyMap()): V1MockBlockchainBeaconMessage =
                V1MockBlockchainBeaconMessage(type, version, id, beaconId, rest, MockMessageType.Request)
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
                val mockType = MockMessageType.from(type) ?: failWithIllegalArgument()

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

    @Serializable(with = V2MockPermissionBeaconRequest.Serializer::class)
    internal data class V2MockPermissionBeaconRequest(
        override val type: String,
        override val version: String,
        override val id: String,
        override val senderId: String,
        val appMetadata: V2AppMetadata,
        val rest: Map<String, JsonElement>,
    ) : V2BeaconMessage() {
        override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
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
        val publicKey: String,
        val threshold: Threshold?,
        val rest: Map<String, JsonElement>,
    ) : V2BeaconMessage() {
        override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
            PermissionMockResponse(
                type,
                id,
                version,
                origin,
                MockBlockchain.IDENTIFIER,
                publicKey,
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
                element<String>("publicKey")
                element<Threshold>("threshold", isOptional = true)
            }

            override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V2MockPermissionBeaconResponse {
                val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))
                val version = jsonElement.jsonObject.getString(descriptor.getElementName(1))
                val id = jsonElement.jsonObject.getString(descriptor.getElementName(2))
                val senderId = jsonElement.jsonObject.getString(descriptor.getElementName(3))
                val publicKey = jsonElement.jsonObject.getString(descriptor.getElementName(4))
                val threshold = jsonElement.jsonObject.getSerializableOrNull(descriptor.getElementName(5), jsonDecoder, Threshold.serializer())
                val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }

                return V2MockPermissionBeaconResponse(type, version, id, senderId, publicKey, threshold, rest)
            }

            override fun serialize(jsonEncoder: JsonEncoder, value: V2MockPermissionBeaconResponse) {
                val fields = mapOf(
                    descriptor.getElementName(0) to JsonPrimitive(value.type),
                    descriptor.getElementName(1) to JsonPrimitive(value.version),
                    descriptor.getElementName(2) to JsonPrimitive(value.id),
                    descriptor.getElementName(3) to JsonPrimitive(value.senderId),
                    descriptor.getElementName(4) to JsonPrimitive(value.publicKey),
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
        val mockType: MockMessageType
    ) : V2BeaconMessage() {

        override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
            when (mockType) {
                MockMessageType.Request -> {
                    val appMetadata = storageManager.findAppMetadata { it.senderId == senderId }
                    BlockchainMockRequest(
                        type,
                        id,
                        version,
                        MockBlockchain.IDENTIFIER,
                        senderId,
                        appMetadata,
                        origin,
                        rest,
                    )
                }
                MockMessageType.Response -> BlockchainMockResponse(type, id, version, origin, MockBlockchain.IDENTIFIER, rest)
            }

        companion object {
            fun response(type: String, version: String, id: String, senderId: String, rest: Map<String, JsonElement> = emptyMap()): V2MockBlockchainBeaconMessage =
                V2MockBlockchainBeaconMessage(type, version, id, senderId, rest, MockMessageType.Response)

            fun request(type: String, version: String, id: String, senderId: String, rest: Map<String, JsonElement> = emptyMap()): V2MockBlockchainBeaconMessage =
                V2MockBlockchainBeaconMessage(type, version, id, senderId, rest, MockMessageType.Request)
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
                val mockType = MockMessageType.from(type) ?: failWithIllegalArgument()

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

    @Serializable
    public data class PermissionMockRequest(
        val type: String,
        override val id: String,
        override val version: String,
        override val blockchainIdentifier: String,
        override val senderId: String,
        override val origin: Origin,
        override val appMetadata: AppMetadata,
        val rest: Map<String, JsonElement> = emptyMap(),
    ) : PermissionBeaconRequest() {
        public fun toV1(): V1BeaconMessage =
            V1MockPermissionBeaconRequest(
                type,
                version,
                id,
                senderId,
                V1AppMetadata.fromAppMetadata(appMetadata),
                rest,
            )

        public fun toV2(): V2BeaconMessage =
            V2MockPermissionBeaconRequest(
                type,
                version,
                id,
                senderId,
                V2AppMetadata.fromAppMetadata(appMetadata),
                rest,
            )
    }

    @Serializable
    public data class BlockchainMockRequest(
        val type: String,
        override val id: String,
        override val version: String,
        override val blockchainIdentifier: String,
        override val senderId: String,
        override val appMetadata: AppMetadata?,
        override val origin: Origin,
        val rest: Map<String, JsonElement> = emptyMap(),
    ) : BlockchainBeaconRequest() {
        public fun toV1(): V1BeaconMessage =
            V1MockBlockchainBeaconMessage(
                type,
                version,
                id,
                senderId,
                rest,
                MockMessageType.Request,
            )

        public fun toV2(): V2BeaconMessage =
            V2MockBlockchainBeaconMessage(
                type,
                version,
                id,
                senderId,
                rest,
                MockMessageType.Request,
            )
    }

    @Serializable
    public data class PermissionMockResponse(
        val type: String,
        override val id: String,
        override val version: String,
        override val requestOrigin: Origin,
        override val blockchainIdentifier: String,
        override val publicKey: String,
        override val threshold: Threshold?,
        val rest: Map<String, JsonElement> = emptyMap(),
    ) : PermissionBeaconResponse() {
        public fun toV1(senderId: String): V1BeaconMessage =
            V1MockPermissionBeaconResponse(
                type,
                version,
                id,
                senderId,
                publicKey,
                threshold,
                rest,
            )

        public fun toV2(senderId: String): V2BeaconMessage =
            V2MockPermissionBeaconResponse(
                type,
                version,
                id,
                senderId,
                publicKey,
                threshold,
                rest,
            )
    }

    @Serializable
    public data class BlockchainMockResponse(
        val type: String,
        override val id: String,
        override val version: String,
        override val requestOrigin: Origin,
        override val blockchainIdentifier: String,
        val rest: Map<String, JsonElement> = emptyMap(),
    ) : BlockchainBeaconResponse() {
        public fun toV1(senderId: String): V1BeaconMessage =
            V1MockBlockchainBeaconMessage(
                type,
                version,
                id,
                senderId,
                rest,
                MockMessageType.Response,
            )

        public fun toV2(senderId: String): V2BeaconMessage =
            V2MockBlockchainBeaconMessage(
                type,
                version,
                id,
                senderId,
                rest,
                MockMessageType.Response,
            )
    }

    @Serializable
    public data class MockPermission(
        override val blockchainIdentifier: String,
        override val accountIdentifier: String,
        override val address: String,
        override val senderId: String,
        override val appMetadata: AppMetadata,
        override val publicKey: String,
        override val connectedAt: Long,
        override val threshold: Threshold? = null,
        val rest: Map<String, JsonElement> = emptyMap(),
    ) : Permission()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Serializable
    public sealed class MockError : BeaconError()
}