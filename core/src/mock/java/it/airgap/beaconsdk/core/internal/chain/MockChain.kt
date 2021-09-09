package it.airgap.beaconsdk.core.internal.chain

import it.airgap.beaconsdk.core.data.beacon.Origin
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
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

internal class MockChain : Chain<MockChainWallet, MockChainVersionedMessage> {
    override val wallet: MockChainWallet = MockChainWallet()
    override val versionedMessage: MockChainVersionedMessage = MockChainVersionedMessage()

    class Factory : Chain.Factory<MockChain> {
        override val identifier: String = IDENTIFIER
        override fun create(dependencyRegistry: DependencyRegistry): MockChain = MockChain()
    }

    companion object {
        const val IDENTIFIER = "mockChain"
    }
}

internal class MockChainWallet : Chain.Wallet {
    override fun addressFromPublicKey(publicKey: String): Result<String> =
        Result.success("@$publicKey")
}

internal class MockChainVersionedMessage : Chain.VersionedMessage {
    override val v1: VersionedBeaconMessage.Factory<BeaconMessage, V1BeaconMessage> =
        object : VersionedBeaconMessage.Factory<BeaconMessage, V1BeaconMessage> {
            override fun serializer(): KSerializer<V1BeaconMessage> =
                object : KSerializer<V1BeaconMessage> {
                    private val fieldType: String = "type"
                    private val fieldVersion: String = "version"
                    private val fieldId: String = "id"
                    private val fieldBeaconId: String = "beaconId"

                    private val knownFields: Set<String> = setOf(fieldType, fieldVersion, fieldId, fieldBeaconId)

                    override val descriptor: SerialDescriptor =
                        PrimitiveSerialDescriptor("MockChainV1BeaconMessage", PrimitiveKind.STRING)

                    override fun deserialize(decoder: Decoder): V1BeaconMessage {
                        val jsonDecoder = decoder as? JsonDecoder ?: failWithExpectedJsonDecoder(decoder::class)
                        val jsonElement = jsonDecoder.decodeJsonElement()

                        val type = jsonElement.jsonObject[fieldType]?.jsonPrimitive?.content ?: failWithMissingField(fieldType)
                        val version = jsonElement.jsonObject[fieldVersion]?.jsonPrimitive?.content ?: failWithMissingField(fieldVersion)
                        val id = jsonElement.jsonObject[fieldId]?.jsonPrimitive?.content ?: failWithMissingField(fieldId)
                        val beaconId = jsonElement.jsonObject[fieldBeaconId]?.jsonPrimitive?.content ?: failWithMissingField(fieldBeaconId)
                        val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }
                        val mockType = MockMessageType.from(type) ?: failWithIllegalArgument()

                        return V1MockBeaconMessage(type, version, id, beaconId, rest, mockType)
                    }

                    override fun serialize(encoder: Encoder, value: V1BeaconMessage) {
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

                        encoder.encodeSerializableValue(JsonObject.serializer(), (fields + rest).toJsonObject())
                    }
                }

            override fun from(senderId: String, content: BeaconMessage): V1BeaconMessage =
                when (content) {
                    is ChainBeaconRequest -> when (val payload = content.payload) {
                        is RequestPayload -> V1MockBeaconMessage(payload.type, content.version, content.id, content.senderId, payload.content, MockMessageType.Request)
                        else -> V1MockBeaconMessage(MockMessageType.Request.value, content.version, content.id, content.senderId, emptyMap(), MockMessageType.Request)
                    }
                    is ChainBeaconResponse -> when (val payload = content.payload) {
                        is ResponsePayload -> V1MockBeaconMessage(payload.type, content.version, content.id, senderId, payload.content, MockMessageType.Response)
                        else -> V1MockBeaconMessage(MockMessageType.Response.value, content.version, content.id, senderId, emptyMap(), MockMessageType.Response)
                    }
                    else -> failWithUnsupportedMessage(content, content.version)
                }

        }

    override val v2: VersionedBeaconMessage.Factory<BeaconMessage, V2BeaconMessage> =
        object : VersionedBeaconMessage.Factory<BeaconMessage, V2BeaconMessage> {
            override fun serializer(): KSerializer<V2BeaconMessage> =
                object : KSerializer<V2BeaconMessage> {
                    private val fieldType: String = "type"
                    private val fieldVersion: String = "version"
                    private val fieldId: String = "id"
                    private val fieldSenderId: String = "senderId"

                    private val knownFields: Set<String> = setOf(fieldType, fieldVersion, fieldId, fieldSenderId)

                    override val descriptor: SerialDescriptor =
                        PrimitiveSerialDescriptor("MockChainV2BeaconMessage", PrimitiveKind.STRING)

                    override fun deserialize(decoder: Decoder): V2BeaconMessage {
                        val jsonDecoder = decoder as? JsonDecoder ?: failWithExpectedJsonDecoder(decoder::class)
                        val jsonElement = jsonDecoder.decodeJsonElement()

                        val type = jsonElement.jsonObject[fieldType]?.jsonPrimitive?.content ?: failWithMissingField(fieldType)
                        val version = jsonElement.jsonObject[fieldVersion]?.jsonPrimitive?.content ?: failWithMissingField(fieldVersion)
                        val id = jsonElement.jsonObject[fieldId]?.jsonPrimitive?.content ?: failWithMissingField(fieldId)
                        val senderId = jsonElement.jsonObject[fieldSenderId]?.jsonPrimitive?.content ?: failWithMissingField(fieldSenderId)
                        val rest = jsonElement.jsonObject.filterKeys { !knownFields.contains(it) }
                        val mockType = MockMessageType.from(type) ?: failWithIllegalArgument()

                        return V2MockBeaconMessage(type, version, id, senderId, rest, mockType)
                    }

                    override fun serialize(encoder: Encoder, value: V2BeaconMessage) {
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

                        encoder.encodeSerializableValue(JsonObject.serializer(), (fields + rest).toJsonObject())
                    }
                }

            override fun from(senderId: String, content: BeaconMessage): V2BeaconMessage =
                when (content) {
                    is ChainBeaconRequest -> when (val payload = content.payload) {
                        is RequestPayload -> V2MockBeaconMessage(payload.type, content.version, content.id, content.senderId, payload.content, MockMessageType.Request)
                        else -> V2MockBeaconMessage(MockMessageType.Request.value, content.version, content.id, content.senderId, emptyMap(), MockMessageType.Request)
                    }
                    is ChainBeaconResponse -> when (val payload = content.payload) {
                        is ResponsePayload -> V2MockBeaconMessage(payload.type, content.version, content.id, senderId, payload.content, MockMessageType.Response)
                        else -> V2MockBeaconMessage(MockMessageType.Response.value, content.version, content.id, senderId, emptyMap(), MockMessageType.Response)
                    }
                    else -> failWithIllegalArgument()
                }

        }

    enum class MockMessageType(val value: String) {
        Request("request"),
        Response("response");

        companion object {
            fun from(string: String): MockMessageType? = values().firstOrNull { string.contains(it.value) }
        }
    }

    data class V1MockBeaconMessage(
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

    data class V2MockBeaconMessage(
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

    data class RequestPayload(val type: String, val content: Map<String, JsonElement> = emptyMap()) : ChainBeaconRequest.Payload()
    data class ResponsePayload(val type: String, val content: Map<String, JsonElement> = emptyMap()) : ChainBeaconResponse.Payload()
}