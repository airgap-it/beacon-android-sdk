package it.airgap.beaconsdk.blockchain.tezos.internal.message.v2

import it.airgap.beaconsdk.blockchain.tezos.Tezos
import it.airgap.beaconsdk.blockchain.tezos.data.TezosNetwork
import it.airgap.beaconsdk.blockchain.tezos.data.TezosPermission
import it.airgap.beaconsdk.blockchain.tezos.data.operation.TezosOperation
import it.airgap.beaconsdk.blockchain.tezos.internal.utils.failWithUnknownMessage
import it.airgap.beaconsdk.blockchain.tezos.message.request.BroadcastTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.OperationTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.PermissionTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.SignPayloadTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.response.BroadcastTezosResponse
import it.airgap.beaconsdk.blockchain.tezos.message.response.OperationTezosResponse
import it.airgap.beaconsdk.blockchain.tezos.message.response.PermissionTezosResponse
import it.airgap.beaconsdk.blockchain.tezos.message.response.SignPayloadTezosResponse
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.data.SigningType
import it.airgap.beaconsdk.core.data.Threshold
import it.airgap.beaconsdk.core.internal.message.v2.V2AppMetadata
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalArgument
import it.airgap.beaconsdk.core.internal.utils.getString
import it.airgap.beaconsdk.core.message.BeaconMessage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.jsonObject

@Serializable(with = V2TezosMessage.Serializer::class)
internal sealed class V2TezosMessage : V2BeaconMessage() {

    companion object {
        fun from(senderId: String, content: BeaconMessage): V2TezosMessage =
            when (content) {
                is PermissionTezosRequest -> PermissionV2TezosRequest(
                    content.version,
                    content.id,
                    content.senderId,
                    V2AppMetadata.fromAppMetadata(content.appMetadata),
                    content.network,
                    content.scopes,
                )
                is OperationTezosRequest -> OperationV2TezosRequest(
                    content.version,
                    content.id,
                    content.senderId,
                    content.network,
                    content.operationDetails,
                    content.sourceAddress,
                )
                is SignPayloadTezosRequest -> SignPayloadV2TezosRequest(
                    content.version,
                    content.id,
                    content.senderId,
                    content.signingType,
                    content.payload,
                    content.sourceAddress,
                )
                is BroadcastTezosRequest -> BroadcastV2TezosRequest(
                    content.version,
                    content.id,
                    content.senderId,
                    content.network,
                    content.signedTransaction,
                )
                is PermissionTezosResponse -> PermissionV2TezosResponse(
                    content.version,
                    content.id,
                    senderId,
                    content.publicKey,
                    content.network,
                    content.scopes,
                    content.threshold,
                )
                is OperationTezosResponse -> OperationV2TezosResponse(
                    content.version,
                    content.id,
                    senderId,
                    content.transactionHash,
                )
                is SignPayloadTezosResponse -> SignPayloadV2TezosResponse(
                    content.version,
                    content.id,
                    senderId,
                    content.signingType,
                    content.signature,
                )
                is BroadcastTezosResponse -> BroadcastV2TezosResponse(
                    content.version,
                    content.id,
                    senderId,
                    content.transactionHash,
                )
                else -> failWithUnknownMessage(content)
            }
    }

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer : KJsonSerializer<V2TezosMessage> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V2TezosMessage") {
            element<String>("type")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V2TezosMessage {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))

            return when (type) {
                PermissionV2TezosRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(PermissionV2TezosRequest.serializer(), jsonElement)
                OperationV2TezosRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(OperationV2TezosRequest.serializer(), jsonElement)
                SignPayloadV2TezosRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(SignPayloadV2TezosRequest.serializer(), jsonElement)
                BroadcastV2TezosRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(BroadcastV2TezosRequest.serializer(), jsonElement)
                PermissionV2TezosResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(PermissionV2TezosResponse.serializer(), jsonElement)
                OperationV2TezosResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(OperationV2TezosResponse.serializer(), jsonElement)
                SignPayloadV2TezosResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(SignPayloadV2TezosResponse.serializer(), jsonElement)
                BroadcastV2TezosResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(BroadcastV2TezosResponse.serializer(), jsonElement)
                else -> failWithUnknownType(type)
            }
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V2TezosMessage) {
            when (value) {
                is PermissionV2TezosRequest -> jsonEncoder.encodeSerializableValue(PermissionV2TezosRequest.serializer(), value)
                is OperationV2TezosRequest -> jsonEncoder.encodeSerializableValue(OperationV2TezosRequest.serializer(), value)
                is SignPayloadV2TezosRequest -> jsonEncoder.encodeSerializableValue(SignPayloadV2TezosRequest.serializer(), value)
                is BroadcastV2TezosRequest -> jsonEncoder.encodeSerializableValue(BroadcastV2TezosRequest.serializer(), value)
                is PermissionV2TezosResponse -> jsonEncoder.encodeSerializableValue(PermissionV2TezosResponse.serializer(), value)
                is OperationV2TezosResponse -> jsonEncoder.encodeSerializableValue(OperationV2TezosResponse.serializer(), value)
                is SignPayloadV2TezosResponse -> jsonEncoder.encodeSerializableValue(SignPayloadV2TezosResponse.serializer(), value)
                is BroadcastV2TezosResponse -> jsonEncoder.encodeSerializableValue(BroadcastV2TezosResponse.serializer(), value)
            }
        }

        private fun failWithUnknownType(type: String): Nothing = failWithIllegalArgument("Unknown Tezos message type $type")
    }
}

@Serializable
internal data class PermissionV2TezosRequest(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val appMetadata: V2AppMetadata,
    val network: TezosNetwork,
    val scopes: List<TezosPermission.Scope>,
) : V2TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        PermissionTezosRequest(id, version, Tezos.IDENTIFIER, senderId, appMetadata.toAppMetadata(), origin, network, scopes)

    companion object {
        const val TYPE = "permission_request"
    }
}

@Serializable
internal data class OperationV2TezosRequest(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val network: TezosNetwork,
    val operationDetails: List<TezosOperation>,
    val sourceAddress: String,
) : V2TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage {
        val appMetadata = storageManager.findAppMetadata { it.senderId == senderId }
        return OperationTezosRequest(
            id,
            version,
            Tezos.IDENTIFIER,
            senderId,
            appMetadata,
            origin,
            network,
            operationDetails,
            sourceAddress,
        )
    }

    companion object {
        const val TYPE = "operation_request"
    }
}

@Serializable
internal data class SignPayloadV2TezosRequest(
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
        return SignPayloadTezosRequest(
            id,
            version,
            Tezos.IDENTIFIER,
            senderId,
            appMetadata,
            origin,
            signingType,
            payload,
            sourceAddress,
        )
    }

    companion object {
        const val TYPE = "sign_payload_request"
    }
}

@Serializable
internal data class BroadcastV2TezosRequest(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val network: TezosNetwork,
    val signedTransaction: String,
) : V2TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage {
        val appMetadata = storageManager.findAppMetadata { it.senderId == senderId }
        return BroadcastTezosRequest(
            id,
            version,
            Tezos.IDENTIFIER,
            senderId,
            appMetadata,
            origin,
            network,
            signedTransaction,
        )
    }

    companion object {
        const val TYPE = "broadcast_request"
    }
}

@Serializable
internal data class PermissionV2TezosResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val publicKey: String,
    val network: TezosNetwork,
    val scopes: List<TezosPermission.Scope>,
    val threshold: Threshold? = null,
) : V2TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        PermissionTezosResponse(id, version, origin, Tezos.IDENTIFIER, publicKey, network, scopes, threshold)

    companion object {
        const val TYPE = "permission_response"
    }
}

@Serializable
internal data class OperationV2TezosResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val transactionHash: String,
) : V2TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        OperationTezosResponse(
            id,
            version,
            origin,
            Tezos.IDENTIFIER,
            transactionHash,
        )

    companion object {
        const val TYPE = "operation_response"
    }
}

@Serializable
internal data class SignPayloadV2TezosResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val signingType: SigningType,
    val signature: String,
) : V2TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        SignPayloadTezosResponse(
            id,
            version,
            origin,
            Tezos.IDENTIFIER,
            signingType,
            signature,
        )

    companion object {
        const val TYPE = "sign_payload_response"
    }
}

@Serializable
internal data class BroadcastV2TezosResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val transactionHash: String,
) : V2TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        BroadcastTezosResponse(
            id,
            version,
            origin,
            Tezos.IDENTIFIER,
            transactionHash,
        )

    companion object {
        const val TYPE = "broadcast_response"
    }
}
