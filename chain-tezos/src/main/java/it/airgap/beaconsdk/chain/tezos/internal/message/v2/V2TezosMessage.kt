package it.airgap.beaconsdk.chain.tezos.internal.message.v2

import it.airgap.beaconsdk.chain.tezos.Tezos
import it.airgap.beaconsdk.chain.tezos.internal.utils.failWithUnknownMessage
import it.airgap.beaconsdk.chain.tezos.internal.utils.failWithUnknownPayload
import it.airgap.beaconsdk.chain.tezos.internal.utils.failWithUnknownType
import it.airgap.beaconsdk.chain.tezos.message.*
import it.airgap.beaconsdk.core.data.beacon.Network
import it.airgap.beaconsdk.core.data.beacon.Origin
import it.airgap.beaconsdk.core.data.beacon.SigningType
import it.airgap.beaconsdk.core.data.tezos.TezosOperation
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
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

@Serializable(with = V2TezosMessage.Serializer::class)
public abstract class V2TezosMessage : V2BeaconMessage() {

    companion object : Factory<BeaconMessage, V2BeaconMessage> {
        override fun from(senderId: String, content: BeaconMessage): V2BeaconMessage =
            when (content) {
                is ChainBeaconRequest -> when (val payload = content.payload) {
                    is OperationTezosRequest -> OperationV2TezosRequest(
                        content.version,
                        content.id,
                        content.senderId,
                        payload.network,
                        payload.operationDetails,
                        payload.sourceAddress,
                    )
                    is SignPayloadTezosRequest -> SignPayloadV2TezosRequest(
                        content.version,
                        content.id,
                        content.senderId,
                        payload.signingType,
                        payload.payload,
                        payload.sourceAddress,
                    )
                    is BroadcastTezosRequest -> BroadcastV2TezosRequest(
                        content.version,
                        content.id,
                        content.senderId,
                        payload.network,
                        payload.signedTransaction,
                    )
                    else -> failWithUnknownPayload(payload)
                }
                is ChainBeaconResponse -> when (val payload = content.payload) {
                    is OperationTezosResponse -> OperationV2TezosResponse(
                        content.version,
                        content.id,
                        senderId,
                        payload.transactionHash,
                    )
                    is SignPayloadTezosResponse -> SignPayloadV2TezosResponse(
                        content.version,
                        content.id,
                        senderId,
                        payload.signingType,
                        payload.signature,
                    )
                    is BroadcastTezosResponse -> BroadcastV2TezosResponse(
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

    object Serializer : KSerializer<V2TezosMessage> {
        object Field {
            const val TYPE = "type"
        }

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("V2TezosMessage", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): V2TezosMessage {
            val jsonDecoder = decoder as? JsonDecoder ?: failWithExpectedJsonDecoder(decoder::class)
            val jsonElement = jsonDecoder.decodeJsonElement()

            val type = jsonElement.jsonObject[Field.TYPE]?.jsonPrimitive?.content ?: failWithMissingField(Field.TYPE)

            return when (type) {
                OperationV2TezosRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(OperationV2TezosRequest.serializer(), jsonElement)
                SignPayloadV2TezosRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(SignPayloadV2TezosRequest.serializer(), jsonElement)
                BroadcastV2TezosRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(BroadcastV2TezosRequest.serializer(), jsonElement)
                OperationV2TezosResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(OperationV2TezosResponse.serializer(), jsonElement)
                SignPayloadV2TezosResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(SignPayloadV2TezosResponse.serializer(), jsonElement)
                BroadcastV2TezosResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(BroadcastV2TezosResponse.serializer(), jsonElement)
                else -> failWithUnknownType(type)
            }
        }

        override fun serialize(encoder: Encoder, value: V2TezosMessage) {
            when (value) {
                is OperationV2TezosRequest -> encoder.encodeSerializableValue(OperationV2TezosRequest.serializer(), value)
                is SignPayloadV2TezosRequest -> encoder.encodeSerializableValue(SignPayloadV2TezosRequest.serializer(), value)
                is BroadcastV2TezosRequest -> encoder.encodeSerializableValue(BroadcastV2TezosRequest.serializer(), value)
                is OperationV2TezosResponse -> encoder.encodeSerializableValue(OperationV2TezosResponse.serializer(), value)
                is SignPayloadV2TezosResponse -> encoder.encodeSerializableValue(SignPayloadV2TezosResponse.serializer(), value)
                is BroadcastV2TezosResponse -> encoder.encodeSerializableValue(BroadcastV2TezosResponse.serializer(), value)
                else -> failWithUnknownType(value.type)
            }
        }
    }
}

@Serializable
public data class OperationV2TezosRequest(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val network: Network,
    val operationDetails: List<TezosOperation>,
    val sourceAddress: String,
) : V2TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage {
        val appMetadata = storageManager.findAppMetadata { it.senderId == senderId }
        return ChainBeaconRequest(
            id,
            senderId,
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
public data class SignPayloadV2TezosRequest(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val signingType: SigningType,
    val payload: String,
    val sourceAddress: String,
) : V2TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage {
        val appMetadata = storageManager.findAppMetadata { it.senderId == senderId }
        return ChainBeaconRequest(
            id,
            senderId,
            appMetadata,
            Tezos.IDENTIFIER,
            SignPayloadTezosRequest(
                signingType,
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
public data class BroadcastV2TezosRequest(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val network: Network,
    val signedTransaction: String,
) : V2TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage {
        val appMetadata = storageManager.findAppMetadata { it.senderId == senderId }
        return ChainBeaconRequest(
            id,
            senderId,
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
public data class OperationV2TezosResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val transactionHash: String,
) : V2TezosMessage() {
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
public data class SignPayloadV2TezosResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val signingType: SigningType,
    val signature: String,
) : V2TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        ChainBeaconResponse(
            id,
            Tezos.IDENTIFIER,
            SignPayloadTezosResponse(
                signingType,
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
public data class BroadcastV2TezosResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val transactionHash: String,
) : V2TezosMessage() {
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
