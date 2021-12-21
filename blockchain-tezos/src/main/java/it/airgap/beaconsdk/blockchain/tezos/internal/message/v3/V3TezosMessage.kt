package it.airgap.beaconsdk.blockchain.tezos.internal.message.v3

import it.airgap.beaconsdk.blockchain.tezos.Tezos
import it.airgap.beaconsdk.blockchain.tezos.data.TezosAppMetadata
import it.airgap.beaconsdk.blockchain.tezos.data.TezosNetwork
import it.airgap.beaconsdk.blockchain.tezos.data.TezosPermission
import it.airgap.beaconsdk.blockchain.tezos.data.operation.TezosOperation
import it.airgap.beaconsdk.blockchain.tezos.message.request.*
import it.airgap.beaconsdk.blockchain.tezos.message.response.*
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.data.SigningType
import it.airgap.beaconsdk.core.internal.message.v3.BlockchainV3BeaconRequestContent
import it.airgap.beaconsdk.core.internal.message.v3.BlockchainV3BeaconResponseContent
import it.airgap.beaconsdk.core.internal.message.v3.PermissionV3BeaconRequestContent
import it.airgap.beaconsdk.core.internal.message.v3.PermissionV3BeaconResponseContent
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.message.BeaconMessage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
internal data class PermissionV3TezosRequest(
    val network: TezosNetwork,
    val appMetadata: V3TezosAppMetadata,
    val scopes: List<TezosPermission.Scope>,
) : PermissionV3BeaconRequestContent.BlockchainData() {

    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        blockchainIdentifier: String,
        storageManager: StorageManager,
        identifierCreator: IdentifierCreator,
    ): BeaconMessage = PermissionTezosRequest(id, version, blockchainIdentifier, senderId, appMetadata.toAppMetadata(), origin, network, scopes)

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
@Serializable
@JsonClassDiscriminator(BlockchainV3TezosRequest.CLASS_DISCRIMINATOR)
internal sealed class BlockchainV3TezosRequest : BlockchainV3BeaconRequestContent.BlockchainData() {
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
}

@Serializable
@SerialName(OperationV3TezosRequest.TYPE)
internal data class OperationV3TezosRequest(
    val network: TezosNetwork,
    val operationDetails: List<TezosOperation>,
    val sourceAddress: String,
) : BlockchainV3TezosRequest() {

    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        accountId: String,
        blockchainIdentifier: String,
        storageManager: StorageManager,
        identifierCreator: IdentifierCreator,
    ): BeaconMessage {
        val appMetadata = storageManager.findInstanceAppMetadata<TezosAppMetadata> { it.senderId == senderId }
        return OperationTezosRequest(
            id,
            version,
            blockchainIdentifier,
            senderId,
            appMetadata,
            origin,
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

    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        accountId: String,
        blockchainIdentifier: String,
        storageManager: StorageManager,
        identifierCreator: IdentifierCreator,
    ): BeaconMessage {
        val appMetadata = storageManager.findInstanceAppMetadata<TezosAppMetadata> { it.senderId == senderId }
        return SignPayloadTezosRequest(
            id,
            version,
            blockchainIdentifier,
            senderId,
            appMetadata,
            origin,
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

    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        accountId: String,
        blockchainIdentifier: String,
        storageManager: StorageManager,
        identifierCreator: IdentifierCreator,
    ): BeaconMessage {
        val appMetadata = storageManager.findInstanceAppMetadata<TezosAppMetadata> { it.senderId == senderId }
        return BroadcastTezosRequest(
            id,
            version,
            blockchainIdentifier,
            senderId,
            appMetadata,
            origin,
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
    val publicKey: String,
    val network: TezosNetwork,
    val scopes: List<TezosPermission.Scope>,
) : PermissionV3BeaconResponseContent.BlockchainData() {

    companion object {
        fun from(permissionResponse: PermissionTezosResponse): PermissionV3TezosResponse = with(permissionResponse) {
            PermissionV3TezosResponse(
                publicKey,
                network,
                scopes,
            )
        }
    }

    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        accountId: String,
        blockchainIdentifier: String,
        storageManager: StorageManager,
        identifierCreator: IdentifierCreator,
    ): BeaconMessage = PermissionTezosResponse(id, version, origin, blockchainIdentifier, accountId, publicKey, network, scopes)
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator(BlockchainV3TezosResponse.CLASS_DISCRIMINATOR)
internal sealed class BlockchainV3TezosResponse : BlockchainV3BeaconResponseContent.BlockchainData() {
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
}

@Serializable
@SerialName(OperationV3TezosResponse.TYPE)
internal data class OperationV3TezosResponse(
    val transactionHash: String,
) : BlockchainV3TezosResponse() {

    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        blockchainIdentifier: String,
        storageManager: StorageManager,
        identifierCreator: IdentifierCreator,
    ): BeaconMessage = OperationTezosResponse(id, version, origin, blockchainIdentifier, transactionHash)

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

    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        blockchainIdentifier: String,
        storageManager: StorageManager,
        identifierCreator: IdentifierCreator,
    ): BeaconMessage = SignPayloadTezosResponse(id, version, origin, blockchainIdentifier, signingType, signature)

    companion object {
        const val TYPE = "sign_payload_response"
    }
}

@Serializable
@SerialName(BroadcastV3TezosResponse.TYPE)
internal data class BroadcastV3TezosResponse(
    val transactionHash: String,
) : BlockchainV3TezosResponse() {

    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        blockchainIdentifier: String,
        storageManager: StorageManager,
        identifierCreator: IdentifierCreator,
    ): BeaconMessage = BroadcastTezosResponse(id, version, origin, Tezos.IDENTIFIER, transactionHash)

    companion object {
        const val TYPE = "broadcast_response"
    }
}