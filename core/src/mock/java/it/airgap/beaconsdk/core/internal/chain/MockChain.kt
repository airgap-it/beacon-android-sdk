package it.airgap.beaconsdk.core.internal.chain

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.*
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.ChainBeaconRequest
import it.airgap.beaconsdk.core.message.ChainBeaconResponse
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.json.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MockChain : Chain<MockChainWallet, MockChainVersionedMessage> {
    override val identifier: String = IDENTIFIER
    override val wallet: MockChainWallet = MockChainWallet()
    override val serializer: MockChainVersionedMessage = MockChainVersionedMessage()

    public class Factory : Chain.Factory<MockChain> {
        override val identifier: String = IDENTIFIER
        override fun create(dependencyRegistry: DependencyRegistry): MockChain = MockChain()
    }

    public companion object {
        public const val IDENTIFIER = "mockChain"
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MockChainWallet : Chain.Wallet {
    override fun addressFromPublicKey(publicKey: String): Result<String> =
        Result.success("@$publicKey")
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MockChainVersionedMessage : Chain.Serializer {
    override val requestPayload: KSerializer<ChainBeaconRequest.Payload>
        get() = PolymorphicSerializer(RequestPayload.serializer())

    override val responsePayload: KSerializer<ChainBeaconResponse.Payload>
        get() = PolymorphicSerializer(ResponsePayload.serializer())

    override val error: KSerializer<BeaconError>
        get() = PolymorphicSerializer(Error.serializer())

    override val v1: VersionedBeaconMessage.Factory<BeaconMessage, V1BeaconMessage> =
        object : VersionedBeaconMessage.Factory<BeaconMessage, V1BeaconMessage> {
            override fun serializer(): KSerializer<V1BeaconMessage> =
                object : KJsonSerializer<V1BeaconMessage> {
                    private val fieldType: String = "type"
                    private val fieldVersion: String = "version"
                    private val fieldId: String = "id"
                    private val fieldBeaconId: String = "beaconId"

                    private val knownFields: Set<String> = setOf(fieldType, fieldVersion, fieldId, fieldBeaconId)

                    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MockChainV1BeaconMessage") {
                        element<String>(fieldType)
                        element<String>(fieldVersion)
                        element<String>(fieldId)
                        element<String>(fieldBeaconId)
                    }

                    override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V1BeaconMessage {
                        val type = jsonElement.jsonObject.getString(fieldType)
                        val version = jsonElement.jsonObject.getString(fieldVersion)
                        val id = jsonElement.jsonObject.getString(fieldId)
                        val beaconId = jsonElement.jsonObject.getString(fieldBeaconId)
                        val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }
                        val mockType = MockMessageType.from(type) ?: failWithIllegalArgument()

                        return V1MockBeaconMessage(type, version, id, beaconId, rest, mockType)
                    }

                    override fun serialize(jsonEncoder: JsonEncoder, value: V1BeaconMessage) {
                        val fields = mapOf<String, JsonElement>(
                            fieldType to JsonPrimitive(value.type),
                            fieldVersion to JsonPrimitive(value.version),
                            fieldId to JsonPrimitive(value.id),
                            fieldBeaconId to JsonPrimitive(value.beaconId),
                        )

                        val rest = when (value) {
                            is V1MockBeaconMessage -> value.rest
                            else -> failWithIllegalArgument()
                        }

                        jsonEncoder.encodeSerializableValue(JsonObject.serializer(), (fields + rest).toJsonObject())
                    }
                }

            override fun from(senderId: String, content: BeaconMessage): V1BeaconMessage =
                when (content) {
                    is ChainBeaconRequest<*> -> when (val payload = content.payload) {
                        is RequestPayload -> V1MockBeaconMessage(payload.type, content.version, content.id, content.senderId, payload.content, MockMessageType.Request)
                        else -> V1MockBeaconMessage(MockMessageType.Request.value, content.version, content.id, content.senderId, emptyMap(), MockMessageType.Request)
                    }
                    is ChainBeaconResponse<*> -> when (val payload = content.payload) {
                        is ResponsePayload -> V1MockBeaconMessage(payload.type, content.version, content.id, senderId, payload.content, MockMessageType.Response)
                        else -> V1MockBeaconMessage(MockMessageType.Response.value, content.version, content.id, senderId, emptyMap(), MockMessageType.Response)
                    }
                    else -> failWithUnsupportedMessage(content, content.version)
                }

        }

    override val v2: VersionedBeaconMessage.Factory<BeaconMessage, V2BeaconMessage> =
        object : VersionedBeaconMessage.Factory<BeaconMessage, V2BeaconMessage> {
            override fun serializer(): KSerializer<V2BeaconMessage> =
                object : KJsonSerializer<V2BeaconMessage> {
                    private val fieldType: String = "type"
                    private val fieldVersion: String = "version"
                    private val fieldId: String = "id"
                    private val fieldSenderId: String = "senderId"

                    private val knownFields: Set<String> = setOf(fieldType, fieldVersion, fieldId, fieldSenderId)

                    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MockChainV2BeaconMessage") {
                        element<String>(fieldType)
                        element<String>(fieldVersion)
                        element<String>(fieldId)
                        element<String>(fieldSenderId)
                    }

                    override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V2BeaconMessage {
                        val type = jsonElement.jsonObject.getString(fieldType)
                        val version = jsonElement.jsonObject.getString(fieldVersion)
                        val id = jsonElement.jsonObject.getString(fieldId)
                        val senderId = jsonElement.jsonObject.getString(fieldSenderId)
                        val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }
                        val mockType = MockMessageType.from(type) ?: failWithIllegalArgument()

                        return V2MockBeaconMessage(type, version, id, senderId, rest, mockType)
                    }

                    override fun serialize(jsonEncoder: JsonEncoder, value: V2BeaconMessage) {
                        val fields = mapOf<String, JsonElement>(
                            fieldType to JsonPrimitive(value.type),
                            fieldVersion to JsonPrimitive(value.version),
                            fieldId to JsonPrimitive(value.id),
                            fieldSenderId to JsonPrimitive(value.senderId),
                        )

                        val rest = when (value) {
                            is V2MockBeaconMessage -> value.rest
                            else -> failWithIllegalArgument()
                        }

                        jsonEncoder.encodeSerializableValue(JsonObject.serializer(), (fields + rest).toJsonObject())
                    }
                }

            override fun from(senderId: String, content: BeaconMessage): V2BeaconMessage =
                when (content) {
                    is ChainBeaconRequest<*> -> when (val payload = content.payload) {
                        is RequestPayload -> V2MockBeaconMessage(payload.type, content.version, content.id, content.senderId, payload.content, MockMessageType.Request)
                        else -> V2MockBeaconMessage(MockMessageType.Request.value, content.version, content.id, content.senderId, emptyMap(), MockMessageType.Request)
                    }
                    is ChainBeaconResponse<*> -> when (val payload = content.payload) {
                        is ResponsePayload -> V2MockBeaconMessage(payload.type, content.version, content.id, senderId, payload.content, MockMessageType.Response)
                        else -> V2MockBeaconMessage(MockMessageType.Response.value, content.version, content.id, senderId, emptyMap(), MockMessageType.Response)
                    }
                    else -> failWithIllegalArgument()
                }

        }

    internal enum class MockMessageType(val value: String) {
        Request("request"),
        Response("response");

        companion object {
            fun from(string: String): MockMessageType? = values().firstOrNull { string.contains(it.value) }
        }
    }

    internal data class V1MockBeaconMessage(
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
                    ChainBeaconRequest(
                        id,
                        beaconId,
                        appMetadata,
                        MockChain.IDENTIFIER,
                        RequestPayload(type, rest),
                        origin,
                        version,
                    )
                }
                MockMessageType.Response -> ChainBeaconResponse(id, MockChain.IDENTIFIER, ResponsePayload(type, rest), version, origin)
            }

        companion object {
            fun response(type: String, version: String, id: String, beaconId: String, rest: Map<String, JsonElement> = emptyMap()): V1MockBeaconMessage =
                V1MockBeaconMessage(type, version, id, beaconId, rest , MockMessageType.Response)

            fun request(type: String, version: String, id: String, beaconId: String, rest: Map<String, JsonElement> = emptyMap()): V1MockBeaconMessage =
                V1MockBeaconMessage(type, version, id, beaconId, rest, MockMessageType.Request)
        }
    }

    internal data class V2MockBeaconMessage(
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
                    ChainBeaconRequest(
                        id,
                        senderId,
                        appMetadata,
                        MockChain.IDENTIFIER,
                        RequestPayload(type, rest),
                        origin,
                        version,
                    )
                }
                MockMessageType.Response -> ChainBeaconResponse(id, MockChain.IDENTIFIER, ResponsePayload(type, rest), version, origin)
            }

        companion object {
            fun response(type: String, version: String, id: String, senderId: String, rest: Map<String, JsonElement> = emptyMap()): V2MockBeaconMessage =
                V2MockBeaconMessage(type, version, id, senderId, rest, MockMessageType.Response)

            fun request(type: String, version: String, id: String, senderId: String, rest: Map<String, JsonElement> = emptyMap()): V2MockBeaconMessage =
                V2MockBeaconMessage(type, version, id, senderId, rest, MockMessageType.Request)
        }
    }

    @Serializable
    internal data class RequestPayload(val type: String, val content: Map<String, JsonElement> = emptyMap()) : ChainBeaconRequest.Payload()

    @Serializable
    internal data class ResponsePayload(val type: String, val content: Map<String, JsonElement> = emptyMap()) : ChainBeaconResponse.Payload()

    @Serializable
    internal sealed class Error : BeaconError()
}