package it.airgap.beaconsdk.blockchain.tezos.internal.message.v3

import it.airgap.beaconsdk.blockchain.tezos.Tezos
import it.airgap.beaconsdk.blockchain.tezos.data.TezosAccount
import it.airgap.beaconsdk.blockchain.tezos.data.TezosAppMetadata
import it.airgap.beaconsdk.blockchain.tezos.data.TezosNetwork
import it.airgap.beaconsdk.blockchain.tezos.data.TezosPermission
import it.airgap.beaconsdk.blockchain.tezos.data.operation.TezosOperation
import it.airgap.beaconsdk.blockchain.tezos.message.request.*
import it.airgap.beaconsdk.blockchain.tezos.message.response.*
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.data.SigningType
import it.airgap.beaconsdk.core.internal.message.v3.BlockchainV3BeaconRequestContent
import it.airgap.beaconsdk.core.internal.message.v3.BlockchainV3BeaconResponseContent
import it.airgap.beaconsdk.core.internal.message.v3.PermissionV3BeaconRequestContent
import it.airgap.beaconsdk.core.internal.message.v3.PermissionV3BeaconResponseContent
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.dependencyRegistry
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalArgument
import it.airgap.beaconsdk.core.internal.utils.getString
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.findAppMetadata
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.json.*

@Serializable
internal data class PermissionV3TezosRequest(
    val network: TezosNetwork,
    val appMetadata: V3TezosAppMetadata,
    val scopes: List<TezosPermission.Scope>,
) : PermissionV3BeaconRequestContent.BlockchainData() {

    override suspend fun toBeaconMessage(
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
        blockchainIdentifier: String,
    ): BeaconMessage = PermissionTezosRequest(id, version, blockchainIdentifier, senderId, appMetadata.toAppMetadata(), origin, destination, network, scopes)

    companion object {
        fun from(permissionRequest: PermissionTezosRequest): PermissionV3TezosRequest = with(permissionRequest) {
            PermissionV3TezosRequest(
                network,
                V3TezosAppMetadata.fromAppMetadata(appMetadata),
                scopes,
            )
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable(with = BlockchainV3TezosRequest.Serializer::class)
@JsonClassDiscriminator(BlockchainV3TezosRequest.CLASS_DISCRIMINATOR)
internal sealed class BlockchainV3TezosRequest : BlockchainV3BeaconRequestContent.BlockchainData() {
    abstract val type: String

    companion object {
        const val CLASS_DISCRIMINATOR = "type"

        fun from(blockchainRequest: BlockchainTezosRequest): BlockchainV3TezosRequest = with(blockchainRequest) {
            when (this) {
                is OperationTezosRequest ->
                    OperationV3TezosRequest(
                        network,
                        operationDetails,
                        sourceAddress,
                    )
                is SignPayloadTezosRequest ->
                    SignPayloadV3TezosRequest(
                        signingType,
                        payload,
                        sourceAddress,
                    )
                is BroadcastTezosRequest ->
                    BroadcastV3TezosRequest(
                        network,
                        signedTransaction,
                    )
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer : KJsonSerializer<BlockchainV3TezosRequest> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("BlockchainV3TezosRequest") {
            element<String>(CLASS_DISCRIMINATOR)
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): BlockchainV3TezosRequest {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))

            return when (type) {
                OperationV3TezosRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(OperationV3TezosRequest.serializer(), jsonElement)
                SignPayloadV3TezosRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(SignPayloadV3TezosRequest.serializer(), jsonElement)
                BroadcastV3TezosRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(BroadcastV3TezosRequest.serializer(), jsonElement)
                else -> failWithUnknownType(type)
            }
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: BlockchainV3TezosRequest) {
            when (value) {
                is OperationV3TezosRequest -> jsonEncoder.encodeSerializableValue(OperationV3TezosRequest.serializer(), value)
                is SignPayloadV3TezosRequest -> jsonEncoder.encodeSerializableValue(SignPayloadV3TezosRequest.serializer(), value)
                is BroadcastV3TezosRequest -> jsonEncoder.encodeSerializableValue(BroadcastV3TezosRequest.serializer(), value)
            }
        }

        private fun failWithUnknownType(type: String): Nothing = failWithIllegalArgument("Unknown Tezos message type $type")
    }
}

@Serializable
@SerialName(OperationV3TezosRequest.TYPE)
internal data class OperationV3TezosRequest(
    val network: TezosNetwork,
    val operationDetails: List<TezosOperation>,
    val sourceAddress: String,
) : BlockchainV3TezosRequest() {
    @Required
    override val type: String = TYPE

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
        val appMetadata = dependencyRegistry(beaconScope).storageManager.findAppMetadata<TezosAppMetadata> { it.senderId == senderId }
        return OperationTezosRequest(
            id,
            version,
            blockchainIdentifier,
            senderId,
            appMetadata,
            origin,
            destination,
            accountId,
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
@SerialName(SignPayloadV3TezosRequest.TYPE)
internal data class SignPayloadV3TezosRequest(
    val signingType: SigningType,
    val payload: String,
    val sourceAddress: String,
) : BlockchainV3TezosRequest() {
    @Required
    override val type: String = TYPE

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
        val appMetadata = dependencyRegistry(beaconScope).storageManager.findAppMetadata<TezosAppMetadata> { it.senderId == senderId }
        return SignPayloadTezosRequest(
            id,
            version,
            blockchainIdentifier,
            senderId,
            appMetadata,
            origin,
            destination,
            accountId,
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
@SerialName(BroadcastV3TezosRequest.TYPE)
internal data class BroadcastV3TezosRequest(
    val network: TezosNetwork,
    val signedTransaction: String,
) : BlockchainV3TezosRequest() {
    @Required
    override val type: String = TYPE

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
        val appMetadata = dependencyRegistry(beaconScope).storageManager.findAppMetadata<TezosAppMetadata> { it.senderId == senderId }
        return BroadcastTezosRequest(
            id,
            version,
            blockchainIdentifier,
            senderId,
            appMetadata,
            origin,
            destination,
            accountId,
            network,
            signedTransaction,
        )
    }

    companion object {
        const val TYPE = "broadcast_request"
    }
}

@Serializable
internal data class PermissionV3TezosResponse(
    val accountId: String,
    val publicKey: String,
    val address: String,
    val network: TezosNetwork,
    val scopes: List<TezosPermission.Scope>,
) : PermissionV3BeaconResponseContent.BlockchainData() {

    companion object {
        fun from(permissionResponse: PermissionTezosResponse): PermissionV3TezosResponse = with(permissionResponse) {
            PermissionV3TezosResponse(
                account.accountId,
                account.publicKey,
                account.address,
                account.network,
                scopes,
            )
        }
    }

    override suspend fun toBeaconMessage(
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
        blockchainIdentifier: String,
    ): BeaconMessage = PermissionTezosResponse(id, version, destination, blockchainIdentifier, TezosAccount(accountId, network, publicKey, address), scopes)
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable(with = BlockchainV3TezosResponse.Serializer::class)
@JsonClassDiscriminator(BlockchainV3TezosResponse.CLASS_DISCRIMINATOR)
internal sealed class BlockchainV3TezosResponse : BlockchainV3BeaconResponseContent.BlockchainData() {
    abstract val type: String

    companion object {
        const val CLASS_DISCRIMINATOR = "type"

        fun from(blockchainResponse: BlockchainTezosResponse): BlockchainV3TezosResponse = with(blockchainResponse) {
            when (this) {
                is OperationTezosResponse -> OperationV3TezosResponse(transactionHash)
                is SignPayloadTezosResponse -> SignPayloadV3TezosResponse(signingType, signature)
                is BroadcastTezosResponse -> BroadcastV3TezosResponse(transactionHash)
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer : KJsonSerializer<BlockchainV3TezosResponse> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("BlockchainV3TezosResponse") {
            element<String>(CLASS_DISCRIMINATOR)
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): BlockchainV3TezosResponse {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))

            return when (type) {
                OperationV3TezosResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(OperationV3TezosResponse.serializer(), jsonElement)
                SignPayloadV3TezosResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(SignPayloadV3TezosResponse.serializer(), jsonElement)
                BroadcastV3TezosResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(BroadcastV3TezosResponse.serializer(), jsonElement)
                else -> failWithUnknownType(type)
            }
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: BlockchainV3TezosResponse) {
            when (value) {
                is OperationV3TezosResponse -> jsonEncoder.encodeSerializableValue(OperationV3TezosResponse.serializer(), value)
                is SignPayloadV3TezosResponse -> jsonEncoder.encodeSerializableValue(SignPayloadV3TezosResponse.serializer(), value)
                is BroadcastV3TezosResponse -> jsonEncoder.encodeSerializableValue(BroadcastV3TezosResponse.serializer(), value)
            }
        }

        private fun failWithUnknownType(type: String): Nothing = failWithIllegalArgument("Unknown Tezos message type $type")
    }
}

@Serializable
@SerialName(OperationV3TezosResponse.TYPE)
internal data class OperationV3TezosResponse(
    val transactionHash: String,
) : BlockchainV3TezosResponse() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
        blockchainIdentifier: String,
    ): BeaconMessage = OperationTezosResponse(id, version, destination, blockchainIdentifier, transactionHash)

    companion object {
        const val TYPE = "operation_response"
    }
}

@Serializable
@SerialName(SignPayloadV3TezosResponse.TYPE)
internal data class SignPayloadV3TezosResponse(
    val signingType: SigningType,
    val signature: String,
) : BlockchainV3TezosResponse() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
        blockchainIdentifier: String,
    ): BeaconMessage = SignPayloadTezosResponse(id, version, destination, blockchainIdentifier, signingType, signature)

    companion object {
        const val TYPE = "sign_payload_response"
    }
}

@Serializable
@SerialName(BroadcastV3TezosResponse.TYPE)
internal data class BroadcastV3TezosResponse(
    val transactionHash: String,
) : BlockchainV3TezosResponse() {
    @Required
    override val type: String = TYPE

    override suspend fun toBeaconMessage(
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
        blockchainIdentifier: String,
    ): BeaconMessage = BroadcastTezosResponse(id, version, destination, Tezos.IDENTIFIER, transactionHash)

    companion object {
        const val TYPE = "broadcast_response"
    }
}