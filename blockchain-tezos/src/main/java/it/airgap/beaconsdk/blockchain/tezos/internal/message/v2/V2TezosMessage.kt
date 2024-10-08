package it.airgap.beaconsdk.blockchain.tezos.internal.message.v2

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
@Serializable(with = V2TezosMessage.Serializer::class)
@JsonClassDiscriminator(V2TezosMessage.CLASS_DISCRIMINATOR)
internal sealed class V2TezosMessage : V2BeaconMessage() {

    companion object {
        const val CLASS_DISCRIMINATOR = "type"
        
        fun from(senderId: String, message: BeaconMessage): V2TezosMessage =
            when (message) {
                is PermissionTezosRequest -> PermissionV2TezosRequest(
                    message.version,
                    message.id,
                    message.senderId,
                    V2TezosAppMetadata.fromAppMetadata(message.appMetadata),
                    message.network,
                    message.scopes,
                )
                is OperationTezosRequest -> OperationV2TezosRequest(
                    message.version,
                    message.id,
                    message.senderId,
                    message.network,
                    message.operationDetails,
                    message.sourceAddress,
                )
                is SignPayloadTezosRequest -> SignPayloadV2TezosRequest(
                    message.version,
                    message.id,
                    message.senderId,
                    message.signingType,
                    message.payload,
                    message.sourceAddress,
                )
                is BroadcastTezosRequest -> BroadcastV2TezosRequest(
                    message.version,
                    message.id,
                    message.senderId,
                    message.network,
                    message.signedTransaction,
                )
                is PermissionTezosResponse -> PermissionV2TezosResponse(
                    message.version,
                    message.id,
                    senderId,
                    message.account.publicKey,
                    message.account.network,
                    message.scopes,
                )
                is OperationTezosResponse -> OperationV2TezosResponse(
                    message.version,
                    message.id,
                    senderId,
                    message.transactionHash,
                )
                is SignPayloadTezosResponse -> SignPayloadV2TezosResponse(
                    message.version,
                    message.id,
                    senderId,
                    message.signingType,
                    message.signature,
                )
                is BroadcastTezosResponse -> BroadcastV2TezosResponse(
                    message.version,
                    message.id,
                    senderId,
                    message.transactionHash,
                )
                else -> failWithUnknownMessage(message)
            }
    }

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer : KJsonSerializer<V2TezosMessage> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("V2TezosMessage") {
            element<String>(CLASS_DISCRIMINATOR)
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

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName(PermissionV2TezosRequest.TYPE)
internal data class PermissionV2TezosRequest(
    @EncodeDefault override val version: String = VERSION,
    override val id: String,
    override val senderId: String,
    val appMetadata: V2TezosAppMetadata,
    val network: TezosNetwork,
    val scopes: List<TezosPermission.Scope>,
) : V2TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Connection.Id, destination: Connection.Id, beaconScope: BeaconScope): BeaconMessage =
        PermissionTezosRequest(id, version, Tezos.IDENTIFIER, senderId, appMetadata.toAppMetadata(), origin, destination, network, scopes)

    companion object {
        const val TYPE = "permission_request"
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName(OperationV2TezosRequest.TYPE)
internal data class OperationV2TezosRequest(
    @EncodeDefault override val version: String = VERSION,
    override val id: String,
    override val senderId: String,
    val network: TezosNetwork,
    val operationDetails: List<TezosOperation>,
    val sourceAddress: String,
) : V2TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Connection.Id, destination: Connection.Id, beaconScope: BeaconScope): BeaconMessage {
        val appMetadata = dependencyRegistry(beaconScope).storageManager.findAppMetadata<TezosAppMetadata> { it.senderId == senderId }
        return OperationTezosRequest(
            id,
            version,
            Tezos.IDENTIFIER,
            senderId,
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
@SerialName(SignPayloadV2TezosRequest.TYPE)
internal data class SignPayloadV2TezosRequest(
    @EncodeDefault override val version: String = VERSION,
    override val id: String,
    override val senderId: String,
    val signingType: SigningType,
    val payload: String,
    val sourceAddress: String,
) : V2TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Connection.Id, destination: Connection.Id, beaconScope: BeaconScope): BeaconMessage {
        val appMetadata = dependencyRegistry(beaconScope).storageManager.findAppMetadata<TezosAppMetadata> { it.senderId == senderId }
        return SignPayloadTezosRequest(
            id,
            version,
            Tezos.IDENTIFIER,
            senderId,
            appMetadata,
            origin,
            destination,
            null,
            signingType,
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
@SerialName(BroadcastV2TezosRequest.TYPE)
internal data class BroadcastV2TezosRequest(
    @EncodeDefault override val version: String = VERSION,
    override val id: String,
    override val senderId: String,
    val network: TezosNetwork,
    val signedTransaction: String,
) : V2TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Connection.Id, destination: Connection.Id, beaconScope: BeaconScope): BeaconMessage {
        val appMetadata = dependencyRegistry(beaconScope).storageManager.findAppMetadata<TezosAppMetadata> { it.senderId == senderId }
        return BroadcastTezosRequest(
            id,
            version,
            Tezos.IDENTIFIER,
            senderId,
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
@SerialName(PermissionV2TezosResponse.TYPE)
internal data class PermissionV2TezosResponse(
    @EncodeDefault override val version: String = VERSION,
    override val id: String,
    override val senderId: String,
    val publicKey: String,
    val network: TezosNetwork,
    val scopes: List<TezosPermission.Scope>,
) : V2TezosMessage() {
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
            scopes,
        )
    }

    companion object {
        const val TYPE = "permission_response"
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName(OperationV2TezosResponse.TYPE)
internal data class OperationV2TezosResponse(
    @EncodeDefault override val version: String = VERSION,
    override val id: String,
    override val senderId: String,
    val transactionHash: String,
) : V2TezosMessage() {
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
@SerialName(SignPayloadV2TezosResponse.TYPE)
internal data class SignPayloadV2TezosResponse(
    @EncodeDefault override val version: String = VERSION,
    override val id: String,
    override val senderId: String,
    val signingType: SigningType,
    val signature: String,
) : V2TezosMessage() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Connection.Id, destination: Connection.Id, beaconScope: BeaconScope): BeaconMessage =
        SignPayloadTezosResponse(
            id,
            version,
            destination,
            Tezos.IDENTIFIER,
            signingType,
            signature,
        )

    companion object {
        const val TYPE = "sign_payload_response"
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName(BroadcastV2TezosResponse.TYPE)
internal data class BroadcastV2TezosResponse(
    @EncodeDefault override val version: String = VERSION,
    override val id: String,
    override val senderId: String,
    val transactionHash: String,
) : V2TezosMessage() {
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
