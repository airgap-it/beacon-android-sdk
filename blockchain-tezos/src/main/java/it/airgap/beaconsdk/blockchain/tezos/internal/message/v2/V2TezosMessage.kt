package it.airgap.beaconsdk.blockchain.tezos.internal.message.v2

import it.airgap.beaconsdk.blockchain.tezos.Tezos
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
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.data.SigningType
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.utils.dependencyRegistry
import it.airgap.beaconsdk.core.message.BeaconMessage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
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
                    message.publicKey,
                    message.network,
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
}

@Serializable
@SerialName(PermissionV2TezosRequest.TYPE)
internal data class PermissionV2TezosRequest(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val appMetadata: V2TezosAppMetadata,
    val network: TezosNetwork,
    val scopes: List<TezosPermission.Scope>,
) : V2TezosMessage() {
    @Transient
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin): BeaconMessage =
        PermissionTezosRequest(id, version, Tezos.IDENTIFIER, senderId, appMetadata.toAppMetadata(), origin, network, scopes)

    companion object {
        const val TYPE = "permission_request"
    }
}

@Serializable
@SerialName(OperationV2TezosRequest.TYPE)
internal data class OperationV2TezosRequest(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val network: TezosNetwork,
    val operationDetails: List<TezosOperation>,
    val sourceAddress: String,
) : V2TezosMessage() {
    @Transient
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin): BeaconMessage {
        val appMetadata = dependencyRegistry.storageManager.findInstanceAppMetadata<TezosAppMetadata> { it.senderId == senderId }
        return OperationTezosRequest(
            id,
            version,
            Tezos.IDENTIFIER,
            senderId,
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
@SerialName(SignPayloadV2TezosRequest.TYPE)
internal data class SignPayloadV2TezosRequest(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val signingType: SigningType,
    val payload: String,
    val sourceAddress: String,
) : V2TezosMessage() {
    @Transient
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin): BeaconMessage {
        val appMetadata = dependencyRegistry.storageManager.findInstanceAppMetadata<TezosAppMetadata> { it.senderId == senderId }
        return SignPayloadTezosRequest(
            id,
            version,
            Tezos.IDENTIFIER,
            senderId,
            appMetadata,
            origin,
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

@Serializable
@SerialName(BroadcastV2TezosRequest.TYPE)
internal data class BroadcastV2TezosRequest(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val network: TezosNetwork,
    val signedTransaction: String,
) : V2TezosMessage() {
    @Transient
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin): BeaconMessage {
        val appMetadata = dependencyRegistry.storageManager.findInstanceAppMetadata<TezosAppMetadata> { it.senderId == senderId }
        return BroadcastTezosRequest(
            id,
            version,
            Tezos.IDENTIFIER,
            senderId,
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
@SerialName(PermissionV2TezosResponse.TYPE)
internal data class PermissionV2TezosResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val publicKey: String,
    val network: TezosNetwork,
    val scopes: List<TezosPermission.Scope>,
) : V2TezosMessage() {
    @Transient
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin): BeaconMessage {
        val address = dependencyRegistry.extend().tezosWallet.address(publicKey).getOrThrow()
        val accountId = dependencyRegistry.identifierCreator.accountId(address, network).getOrThrow()
        return PermissionTezosResponse(
            id,
            version,
            origin,
            Tezos.IDENTIFIER,
            accountId,
            publicKey,
            network,
            scopes,
        )
    }

    companion object {
        const val TYPE = "permission_response"
    }
}

@Serializable
@SerialName(OperationV2TezosResponse.TYPE)
internal data class OperationV2TezosResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val transactionHash: String,
) : V2TezosMessage() {
    @Transient
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin): BeaconMessage =
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
@SerialName(SignPayloadV2TezosResponse.TYPE)
internal data class SignPayloadV2TezosResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val signingType: SigningType,
    val signature: String,
) : V2TezosMessage() {
    @Transient
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin): BeaconMessage =
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
@SerialName(BroadcastV2TezosResponse.TYPE)
internal data class BroadcastV2TezosResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val transactionHash: String,
) : V2TezosMessage() {
    @Transient
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin): BeaconMessage =
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
