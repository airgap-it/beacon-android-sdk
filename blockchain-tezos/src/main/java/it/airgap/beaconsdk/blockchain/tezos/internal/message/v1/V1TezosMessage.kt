package it.airgap.beaconsdk.blockchain.tezos.internal.message.v1

import it.airgap.beaconsdk.blockchain.tezos.Tezos
import it.airgap.beaconsdk.blockchain.tezos.data.TezosNetwork
import it.airgap.beaconsdk.blockchain.tezos.data.TezosPermission
import it.airgap.beaconsdk.blockchain.tezos.data.operation.TezosOperation
import it.airgap.beaconsdk.blockchain.tezos.internal.message.v2.PermissionV2TezosResponse
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
import it.airgap.beaconsdk.core.internal.message.v1.V1AppMetadata
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
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

@Serializable(with = V1TezosMessage.Serializer::class)
internal sealed class V1TezosMessage : V1BeaconMessage() {

    companion object {
        fun from(senderId: String, content: BeaconMessage): V1TezosMessage =
            when (content) {
                is PermissionTezosRequest -> PermissionV1TezosRequest(
                    content.version,
                    content.id,
                    content.senderId,
                    V1AppMetadata.fromAppMetadata(content.appMetadata),
                    content.network,
                    content.scopes,
                )
                is OperationTezosRequest -> OperationV1TezosRequest(
                    content.version,
                    content.id,
                    content.senderId,
                    content.network,
                    content.operationDetails,
                    content.sourceAddress,
                )
                is SignPayloadTezosRequest -> SignPayloadV1TezosRequest(
                    content.version,
                    content.id,
                    content.senderId,
                    content.payload,
                    content.sourceAddress,
                )
                is BroadcastTezosRequest -> BroadcastV1TezosRequest(
                    content.version,
                    content.id,
                    content.senderId,
                    content.network,
                    content.signedTransaction,
                )
                is PermissionTezosResponse -> PermissionV1TezosResponse(
                    content.version,
                    content.id,
                    senderId,
                    content.publicKey,
                    content.network,
                    content.scopes,
                    content.threshold,
                )
                is OperationTezosResponse -> OperationV1TezosResponse(
                    content.version,
                    content.id,
                    senderId,
                    content.transactionHash,
                )
                is SignPayloadTezosResponse -> SignPayloadV1TezosResponse(
                    content.version,
                    content.id,
                    senderId,
                    content.signature,
                )
                is BroadcastTezosResponse -> BroadcastV1TezosResponse(
                    content.version,
                    content.id,
                    senderId,
                    content.transactionHash,
                )
                else -> failWithUnknownMessage(content)
            }
    }

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer : KJsonSerializer<V1TezosMessage> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V1TezosMessage") {
            element<String>("type")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V1TezosMessage {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))

            return when (type) {
                PermissionV1TezosRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(PermissionV1TezosRequest.serializer(), jsonElement)
                OperationV1TezosRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(OperationV1TezosRequest.serializer(), jsonElement)
                SignPayloadV1TezosRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(SignPayloadV1TezosRequest.serializer(), jsonElement)
                BroadcastV1TezosRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(BroadcastV1TezosRequest.serializer(), jsonElement)
                PermissionV2TezosResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(PermissionV1TezosResponse.serializer(), jsonElement)
                OperationV1TezosResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(OperationV1TezosResponse.serializer(), jsonElement)
                SignPayloadV1TezosResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(SignPayloadV1TezosResponse.serializer(), jsonElement)
                BroadcastV1TezosResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(BroadcastV1TezosResponse.serializer(), jsonElement)
                else -> failWithUnknownType(type)
            }
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: V1TezosMessage) {
            when (value) {
                is PermissionV1TezosRequest -> jsonEncoder.encodeSerializableValue(PermissionV1TezosRequest.serializer(), value)
                is OperationV1TezosRequest -> jsonEncoder.encodeSerializableValue(OperationV1TezosRequest.serializer(), value)
                is SignPayloadV1TezosRequest -> jsonEncoder.encodeSerializableValue(SignPayloadV1TezosRequest.serializer(), value)
                is BroadcastV1TezosRequest -> jsonEncoder.encodeSerializableValue(BroadcastV1TezosRequest.serializer(), value)
                is PermissionV1TezosResponse -> jsonEncoder.encodeSerializableValue(PermissionV1TezosResponse.serializer(), value)
                is OperationV1TezosResponse -> jsonEncoder.encodeSerializableValue(OperationV1TezosResponse.serializer(), value)
                is SignPayloadV1TezosResponse -> jsonEncoder.encodeSerializableValue(SignPayloadV1TezosResponse.serializer(), value)
                is BroadcastV1TezosResponse -> jsonEncoder.encodeSerializableValue(BroadcastV1TezosResponse.serializer(), value)
            }
        }

        private fun failWithUnknownType(type: String): Nothing = failWithIllegalArgument("Unknown Tezos message type $type")
    }
}

@Serializable
internal data class PermissionV1TezosRequest(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val appMetadata: V1AppMetadata,
    val network: TezosNetwork,
    val scopes: List<TezosPermission.Scope>,
) : V1TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager, identifierCreator: IdentifierCreator): BeaconMessage =
        PermissionTezosRequest(id, version, Tezos.IDENTIFIER, beaconId, appMetadata.toAppMetadata(), origin, network, scopes)

    companion object {
        const val TYPE = "permission_request"
    }
}

@Serializable
internal data class OperationV1TezosRequest(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val network: TezosNetwork,
    val operationDetails: List<TezosOperation>,
    val sourceAddress: String,
) : V1TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager, identifierCreator: IdentifierCreator): BeaconMessage {
        val appMetadata = storageManager.findAppMetadata { it.senderId == beaconId }
        return OperationTezosRequest(
            id,
            version,
            Tezos.IDENTIFIER,
            beaconId,
            appMetadata,
            origin,
            null,
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
internal data class SignPayloadV1TezosRequest(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val payload: String,
    val sourceAddress: String,
) : V1TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager, identifierCreator: IdentifierCreator): BeaconMessage {
        val appMetadata = storageManager.findAppMetadata { it.senderId == beaconId }
        return SignPayloadTezosRequest(
            id,
            version,
            Tezos.IDENTIFIER,
            beaconId,
            appMetadata,
            origin,
            null,
            SigningType.Raw,
            payload,
            sourceAddress,
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
    val network: TezosNetwork,
    val signedTransaction: String,
) : V1TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager, identifierCreator: IdentifierCreator): BeaconMessage {
        val appMetadata = storageManager.findAppMetadata { it.senderId == beaconId }
        return BroadcastTezosRequest(
            id,
            version,
            Tezos.IDENTIFIER,
            beaconId,
            appMetadata,
            origin,
            null,
            network,
            signedTransaction,
        )
    }

    companion object {
        const val TYPE = "broadcast_request"
    }
}

@Serializable
internal data class PermissionV1TezosResponse(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val publicKey: String,
    val network: TezosNetwork,
    val scopes: List<TezosPermission.Scope>,
    val threshold: Threshold? = null,
) : V1TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager, identifierCreator: IdentifierCreator): BeaconMessage {
        val accountId = identifierCreator.accountId(Tezos.IDENTIFIER, publicKey, network).getOrThrow()
        return PermissionTezosResponse(
            id,
            version,
            origin,
            Tezos.IDENTIFIER,
            accountId,
            publicKey,
            network,
            scopes,
            threshold,
        )
    }

    companion object {
        const val TYPE = "permission_response"
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

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager, identifierCreator: IdentifierCreator): BeaconMessage =
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
internal data class SignPayloadV1TezosResponse(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val signature: String,
) : V1TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager, identifierCreator: IdentifierCreator): BeaconMessage =
        SignPayloadTezosResponse(
            id,
            version,
            origin,
            Tezos.IDENTIFIER,
            SigningType.Raw,
            signature,
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

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager, identifierCreator: IdentifierCreator): BeaconMessage =
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
