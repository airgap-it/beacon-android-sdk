package it.airgap.beaconsdk.blockchain.tezos.internal.message.v1

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
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.utils.dependencyRegistry
import it.airgap.beaconsdk.core.message.BeaconMessage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
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
                    message.publicKey,
                    message.network,
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
}

@Serializable
@SerialName(PermissionV1TezosRequest.TYPE)
internal data class PermissionV1TezosRequest(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val appMetadata: V1TezosAppMetadata,
    val network: TezosNetwork,
    val scopes: List<TezosPermission.Scope>,
) : V1TezosMessage() {
    @Transient
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin): BeaconMessage =
        PermissionTezosRequest(id, version, Tezos.IDENTIFIER, beaconId, appMetadata.toAppMetadata(), origin, network, scopes)

    companion object {
        const val TYPE = "permission_request"
    }
}

@Serializable
@SerialName(OperationV1TezosRequest.TYPE)
internal data class OperationV1TezosRequest(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val network: TezosNetwork,
    val operationDetails: List<TezosOperation>,
    val sourceAddress: String,
) : V1TezosMessage() {
    @Transient
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin): BeaconMessage {
        val appMetadata = dependencyRegistry.storageManager.findInstanceAppMetadata<TezosAppMetadata> { it.senderId == beaconId }
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
@SerialName(SignPayloadV1TezosRequest.TYPE)
internal data class SignPayloadV1TezosRequest(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val payload: String,
    val sourceAddress: String,
) : V1TezosMessage() {
    @Transient
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin): BeaconMessage {
        val appMetadata = dependencyRegistry.storageManager.findInstanceAppMetadata<TezosAppMetadata> { it.senderId == beaconId }
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
@SerialName(BroadcastV1TezosRequest.TYPE)
internal data class BroadcastV1TezosRequest(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val network: TezosNetwork,
    val signedTransaction: String,
) : V1TezosMessage() {
    @Transient
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin): BeaconMessage {
        val appMetadata = dependencyRegistry.storageManager.findInstanceAppMetadata<TezosAppMetadata> { it.senderId == beaconId }
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
@SerialName(PermissionV1TezosResponse.TYPE)
internal data class PermissionV1TezosResponse(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val publicKey: String,
    val network: TezosNetwork,
    val scopes: List<TezosPermission.Scope>
) : V1TezosMessage() {
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
            scopes
        )
    }

    companion object {
        const val TYPE = "permission_response"
    }
}

@Serializable
@SerialName(OperationV1TezosResponse.TYPE)
internal data class OperationV1TezosResponse(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val transactionHash: String,
) : V1TezosMessage() {
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
@SerialName(SignPayloadV1TezosResponse.TYPE)
internal data class SignPayloadV1TezosResponse(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val signature: String,
) : V1TezosMessage() {
    @Transient
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin): BeaconMessage =
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
@SerialName(BroadcastV1TezosResponse.TYPE)
internal data class BroadcastV1TezosResponse(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val transactionHash: String,
) : V1TezosMessage() {
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
