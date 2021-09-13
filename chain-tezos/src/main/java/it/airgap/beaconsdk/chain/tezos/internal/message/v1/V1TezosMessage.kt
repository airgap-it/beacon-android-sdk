package it.airgap.beaconsdk.chain.tezos.internal.message.v1

import it.airgap.beaconsdk.chain.tezos.Tezos
import it.airgap.beaconsdk.chain.tezos.internal.utils.failWithUnknownMessage
import it.airgap.beaconsdk.chain.tezos.internal.utils.failWithUnknownPayload
import it.airgap.beaconsdk.chain.tezos.internal.utils.failWithUnknownType
import it.airgap.beaconsdk.chain.tezos.message.*
import it.airgap.beaconsdk.core.data.beacon.Network
import it.airgap.beaconsdk.core.data.beacon.Origin
import it.airgap.beaconsdk.core.data.beacon.SigningType
import it.airgap.beaconsdk.core.data.tezos.TezosOperation
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.failWithExpectedJsonDecoder
import it.airgap.beaconsdk.core.internal.utils.failWithMissingField
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.ChainBeaconRequest
import it.airgap.beaconsdk.core.message.ChainBeaconResponse
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = V1TezosMessage.Serializer::class)
internal abstract class V1TezosMessage : V1BeaconMessage() {

    companion object : Factory<BeaconMessage, V1BeaconMessage> {
        override fun from(senderId: String, content: BeaconMessage): V1BeaconMessage =
            when (content) {
                is ChainBeaconRequest -> when (val payload = content.payload) {
                    is OperationTezosRequest -> OperationV1TezosRequest(
                        content.version,
                        content.id,
                        content.senderId,
                        payload.network,
                        payload.operationDetails,
                        payload.sourceAddress,
                    )
                    is SignPayloadTezosRequest -> SignPayloadV1TezosRequest(
                        content.version,
                        content.id,
                        content.senderId,
                        payload.payload,
                        payload.sourceAddress,
                    )
                    is BroadcastTezosRequest -> BroadcastV1TezosRequest(
                        content.version,
                        content.id,
                        content.senderId,
                        payload.network,
                        payload.signedTransaction,
                    )
                    else -> failWithUnknownPayload(payload)
                }
                is ChainBeaconResponse -> when (val payload = content.payload) {
                    is OperationTezosResponse -> OperationV1TezosResponse(
                        content.version,
                        content.id,
                        senderId,
                        payload.transactionHash,
                    )
                    is SignPayloadTezosResponse -> SignPayloadV1TezosResponse(
                        content.version,
                        content.id,
                        senderId,
                        payload.signature,
                    )
                    is BroadcastTezosResponse -> BroadcastV1TezosResponse(
                        content.version,
                        content.id,
                        senderId,
                        payload.transactionHash,
                    )
                    else -> failWithUnknownPayload(payload)
                }
                else -> failWithUnknownMessage(content)
            }
    }

    object Serializer : KSerializer<V1TezosMessage> {
        object Field {
            const val TYPE = "type"
        }

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("V1TezosMessage", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): V1TezosMessage {
            val jsonDecoder = decoder as? JsonDecoder ?: failWithExpectedJsonDecoder(decoder::class)
            val jsonElement = jsonDecoder.decodeJsonElement()

            val type = jsonElement.jsonObject[Field.TYPE]?.jsonPrimitive?.content ?: failWithMissingField(Field.TYPE)

            return when (type) {
                OperationV1TezosRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(OperationV1TezosRequest.serializer(), jsonElement)
                SignPayloadV1TezosRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(SignPayloadV1TezosRequest.serializer(), jsonElement)
                BroadcastV1TezosRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(BroadcastV1TezosRequest.serializer(), jsonElement)
                OperationV1TezosResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(OperationV1TezosResponse.serializer(), jsonElement)
                SignPayloadV1TezosResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(SignPayloadV1TezosResponse.serializer(), jsonElement)
                BroadcastV1TezosResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(BroadcastV1TezosResponse.serializer(), jsonElement)
                else -> failWithUnknownType(type)
            }
        }

        override fun serialize(encoder: Encoder, value: V1TezosMessage) {
            when (value) {
                is OperationV1TezosRequest -> encoder.encodeSerializableValue(OperationV1TezosRequest.serializer(), value)
                is SignPayloadV1TezosRequest -> encoder.encodeSerializableValue(SignPayloadV1TezosRequest.serializer(), value)
                is BroadcastV1TezosRequest -> encoder.encodeSerializableValue(BroadcastV1TezosRequest.serializer(), value)
                is OperationV1TezosResponse -> encoder.encodeSerializableValue(OperationV1TezosResponse.serializer(), value)
                is SignPayloadV1TezosResponse -> encoder.encodeSerializableValue(SignPayloadV1TezosResponse.serializer(), value)
                is BroadcastV1TezosResponse -> encoder.encodeSerializableValue(BroadcastV1TezosResponse.serializer(), value)
                else -> failWithUnknownType(value.type)
            }
        }
    }
}

@Serializable
internal data class OperationV1TezosRequest(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val network: Network,
    val operationDetails: List<TezosOperation>,
    val sourceAddress: String,
) : V1TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage {
        val appMetadata = storageManager.findAppMetadata { it.senderId == beaconId }
        return ChainBeaconRequest(
            id,
            beaconId,
            appMetadata,
            Tezos.IDENTIFIER,
            OperationTezosRequest(
                network,
                operationDetails,
                sourceAddress,
            ),
            origin,
            version,
        )
    }

    companion object {
        const val TYPE = "operation_request"
    }
}

@Serializable
internal data class SignPayloadV1TezosRequest(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val payload: String,
    val sourceAddress: String,
) : V1TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage {
        val appMetadata = storageManager.findAppMetadata { it.senderId == beaconId }
        return ChainBeaconRequest(
            id,
            beaconId,
            appMetadata,
            Tezos.IDENTIFIER,
            SignPayloadTezosRequest(
                SigningType.Raw,
                payload,
                sourceAddress,
            ),
            origin,
            version,
        )
    }

    companion object {
        const val TYPE = "sign_payload_request"
    }
}

@Serializable
internal data class BroadcastV1TezosRequest(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val network: Network,
    val signedTransaction: String,
) : V1TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage {
        val appMetadata = storageManager.findAppMetadata { it.senderId == beaconId }
        return ChainBeaconRequest(
            id,
            beaconId,
            appMetadata,
            Tezos.IDENTIFIER,
            BroadcastTezosRequest(
                network,
                signedTransaction,
            ),
            origin,
            version,
        )
    }

    companion object {
        const val TYPE = "broadcast_request"
    }
}

@Serializable
internal data class OperationV1TezosResponse(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val transactionHash: String,
) : V1TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        ChainBeaconResponse(
            id,
            Tezos.IDENTIFIER,
            OperationTezosResponse(transactionHash),
            version,
            origin,
        )

    companion object {
        const val TYPE = "operation_response"
    }
}

@Serializable
internal data class SignPayloadV1TezosResponse(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val signature: String,
) : V1TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        ChainBeaconResponse(
            id,
            Tezos.IDENTIFIER,
            SignPayloadTezosResponse(
                SigningType.Raw,
                signature,
            ),
            version,
            origin,
        )

    companion object {
        const val TYPE = "sign_payload_response"
    }
}

@Serializable
internal data class BroadcastV1TezosResponse(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val transactionHash: String,
) : V1TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        ChainBeaconResponse(
            id,
            Tezos.IDENTIFIER,
            BroadcastTezosResponse(transactionHash),
            version,
            origin,
        )

    companion object {
        const val TYPE = "broadcast_response"
    }
}
