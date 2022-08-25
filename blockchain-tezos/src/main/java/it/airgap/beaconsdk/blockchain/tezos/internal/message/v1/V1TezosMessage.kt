package it.airgap.beaconsdk.blockchain.tezos.internal.message.v1

import it.airgap.beaconsdk.blockchain.tezos.Tezos
import it.airgap.beaconsdk.blockchain.tezos.data.TezosAccount
import it.airgap.beaconsdk.blockchain.tezos.data.TezosAppMetadata
import it.airgap.beaconsdk.blockchain.tezos.data.TezosNetwork
import it.airgap.beaconsdk.blockchain.tezos.data.TezosPermission
import it.airgap.beaconsdk.blockchain.tezos.data.operation.TezosOperation
import it.airgap.beaconsdk.blockchain.tezos.internal.di.extend
import it.airgap.beaconsdk.blockchain.tezos.internal.utils.failWithUnknownMessage
import it.airgap.beaconsdk.blockchain.tezos.message.request.BroadcastTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.OperationTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.PermissionTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.SignPayloadTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.response.BroadcastTezosResponse
import it.airgap.beaconsdk.blockchain.tezos.message.response.OperationTezosResponse
import it.airgap.beaconsdk.blockchain.tezos.message.response.PermissionTezosResponse
import it.airgap.beaconsdk.blockchain.tezos.message.response.SignPayloadTezosResponse
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.data.SigningType
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.dependencyRegistry
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalArgument
import it.airgap.beaconsdk.core.internal.utils.getString
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.findAppMetadata
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.json.*

@OptIn(ExperimentalSerializationApi::class)
@Serializable(with = V1TezosMessage.Serializer::class)
@JsonClassDiscriminator(V1TezosMessage.CLASS_DISCRIMINATOR)
internal sealed class V1TezosMessage : V1BeaconMessage() {

    companion object {
        const val CLASS_DISCRIMINATOR = "type"

        fun from(senderId: String, message: BeaconMessage): V1TezosMessage =
            when (message) {
                is PermissionTezosRequest -> PermissionV1TezosRequest(
                    message.version,
                    message.id,
                    message.senderId,
                    V1TezosAppMetadata.fromAppMetadata(message.appMetadata),
                    message.network,
                    message.scopes,
                )
                is OperationTezosRequest -> OperationV1TezosRequest(
                    message.version,
                    message.id,
                    message.senderId,
                    message.network,
                    message.operationDetails,
                    message.sourceAddress,
                )
                is SignPayloadTezosRequest -> SignPayloadV1TezosRequest(
                    message.version,
                    message.id,
                    message.senderId,
                    message.payload,
                    message.sourceAddress,
                )
                is BroadcastTezosRequest -> BroadcastV1TezosRequest(
                    message.version,
                    message.id,
                    message.senderId,
                    message.network,
                    message.signedTransaction,
                )
                is PermissionTezosResponse -> PermissionV1TezosResponse(
                    message.version,
                    message.id,
                    senderId,
                    message.account.publicKey,
                    message.account.network,
                    message.scopes,
                )
                is OperationTezosResponse -> OperationV1TezosResponse(
                    message.version,
                    message.id,
                    senderId,
                    message.transactionHash,
                )
                is SignPayloadTezosResponse -> SignPayloadV1TezosResponse(
                    message.version,
                    message.id,
                    senderId,
                    message.signature,
                )
                is BroadcastTezosResponse -> BroadcastV1TezosResponse(
                    message.version,
                    message.id,
                    senderId,
                    message.transactionHash,
                )
                else -> failWithUnknownMessage(message)
            }
    }

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer : KJsonSerializer<V1TezosMessage> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V1TezosMessage") {
            element<String>(CLASS_DISCRIMINATOR)
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V1TezosMessage {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))

            return when (type) {
                PermissionV1TezosRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(PermissionV1TezosRequest.serializer(), jsonElement)
                OperationV1TezosRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(OperationV1TezosRequest.serializer(), jsonElement)
                SignPayloadV1TezosRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(SignPayloadV1TezosRequest.serializer(), jsonElement)
                BroadcastV1TezosRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(BroadcastV1TezosRequest.serializer(), jsonElement)
                PermissionV1TezosResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(PermissionV1TezosResponse.serializer(), jsonElement)
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

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName(PermissionV1TezosRequest.TYPE)
internal data class PermissionV1TezosRequest(
    @EncodeDefault override val version: String = VERSION,
    override val id: String,
    override val beaconId: String,
    val appMetadata: V1TezosAppMetadata,
    val network: TezosNetwork,
    val scopes: List<TezosPermission.Scope>,
) : V1TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Connection.Id, destination: Connection.Id, beaconScope: BeaconScope): BeaconMessage =
        PermissionTezosRequest(id, version, Tezos.IDENTIFIER, beaconId, appMetadata.toAppMetadata(), origin, destination, network, scopes)

    companion object {
        const val TYPE = "permission_request"
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName(OperationV1TezosRequest.TYPE)
internal data class OperationV1TezosRequest(
    @EncodeDefault override val version: String = VERSION,
    override val id: String,
    override val beaconId: String,
    val network: TezosNetwork,
    val operationDetails: List<TezosOperation>,
    val sourceAddress: String,
) : V1TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Connection.Id, destination: Connection.Id, beaconScope: BeaconScope): BeaconMessage {
        val appMetadata = dependencyRegistry(beaconScope).storageManager.findAppMetadata<TezosAppMetadata> { it.senderId == beaconId }
        return OperationTezosRequest(
            id,
            version,
            Tezos.IDENTIFIER,
            beaconId,
            appMetadata,
            origin,
            destination,
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

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName(SignPayloadV1TezosRequest.TYPE)
internal data class SignPayloadV1TezosRequest(
    @EncodeDefault override val version: String = VERSION,
    override val id: String,
    override val beaconId: String,
    val payload: String,
    val sourceAddress: String,
) : V1TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Connection.Id, destination: Connection.Id, beaconScope: BeaconScope): BeaconMessage {
        val appMetadata = dependencyRegistry(beaconScope).storageManager.findAppMetadata<TezosAppMetadata> { it.senderId == beaconId }
        return SignPayloadTezosRequest(
            id,
            version,
            Tezos.IDENTIFIER,
            beaconId,
            appMetadata,
            origin,
            destination,
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

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName(BroadcastV1TezosRequest.TYPE)
internal data class BroadcastV1TezosRequest(
    @EncodeDefault override val version: String = VERSION,
    override val id: String,
    override val beaconId: String,
    val network: TezosNetwork,
    val signedTransaction: String,
) : V1TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Connection.Id, destination: Connection.Id, beaconScope: BeaconScope): BeaconMessage {
        val appMetadata = dependencyRegistry(beaconScope).storageManager.findAppMetadata<TezosAppMetadata> { it.senderId == beaconId }
        return BroadcastTezosRequest(
            id,
            version,
            Tezos.IDENTIFIER,
            beaconId,
            appMetadata,
            origin,
            destination,
            null,
            network,
            signedTransaction,
        )
    }

    companion object {
        const val TYPE = "broadcast_request"
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName(PermissionV1TezosResponse.TYPE)
internal data class PermissionV1TezosResponse(
    @EncodeDefault override val version: String = VERSION,
    override val id: String,
    override val beaconId: String,
    val publicKey: String,
    val network: TezosNetwork,
    val scopes: List<TezosPermission.Scope>
) : V1TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Connection.Id, destination: Connection.Id, beaconScope: BeaconScope): BeaconMessage {
        val address = dependencyRegistry(beaconScope).extend().tezosWallet.address(publicKey).getOrThrow()
        val accountId = dependencyRegistry(beaconScope).identifierCreator.accountId(address, network).getOrThrow()
        return PermissionTezosResponse(
            id,
            version,
            destination,
            Tezos.IDENTIFIER,
            TezosAccount(accountId, network, publicKey, address),
            scopes
        )
    }

    companion object {
        const val TYPE = "permission_response"
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName(OperationV1TezosResponse.TYPE)
internal data class OperationV1TezosResponse(
    @EncodeDefault override val version: String = VERSION,
    override val id: String,
    override val beaconId: String,
    val transactionHash: String,
) : V1TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Connection.Id, destination: Connection.Id, beaconScope: BeaconScope): BeaconMessage =
        OperationTezosResponse(
            id,
            version,
            destination,
            Tezos.IDENTIFIER,
            transactionHash,
        )

    companion object {
        const val TYPE = "operation_response"
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName(SignPayloadV1TezosResponse.TYPE)
internal data class SignPayloadV1TezosResponse(
    @EncodeDefault override val version: String = VERSION,
    override val id: String,
    override val beaconId: String,
    val signature: String,
) : V1TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Connection.Id, destination: Connection.Id, beaconScope: BeaconScope): BeaconMessage =
        SignPayloadTezosResponse(
            id,
            version,
            destination,
            Tezos.IDENTIFIER,
            SigningType.Raw,
            signature,
        )

    companion object {
        const val TYPE = "sign_payload_response"
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName(BroadcastV1TezosResponse.TYPE)
internal data class BroadcastV1TezosResponse(
    @EncodeDefault override val version: String = VERSION,
    override val id: String,
    override val beaconId: String,
    val transactionHash: String,
) : V1TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Connection.Id, destination: Connection.Id, beaconScope: BeaconScope): BeaconMessage =
        BroadcastTezosResponse(
            id,
            version,
            destination,
            Tezos.IDENTIFIER,
            transactionHash,
        )

    companion object {
        const val TYPE = "broadcast_response"
    }
}
