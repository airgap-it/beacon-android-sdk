package it.airgap.blockchain.substrate.internal.message.v3

import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.internal.message.v3.BlockchainV3BeaconRequestContent
import it.airgap.beaconsdk.core.internal.message.v3.BlockchainV3BeaconResponseContent
import it.airgap.beaconsdk.core.internal.message.v3.PermissionV3BeaconRequestContent
import it.airgap.beaconsdk.core.internal.message.v3.PermissionV3BeaconResponseContent
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.dependencyRegistry
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalArgument
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.blockchain.substrate.data.*
import it.airgap.blockchain.substrate.message.request.BlockchainSubstrateRequest
import it.airgap.blockchain.substrate.message.request.PermissionSubstrateRequest
import it.airgap.blockchain.substrate.message.request.SignSubstrateRequest
import it.airgap.blockchain.substrate.message.request.TransferSubstrateRequest
import it.airgap.blockchain.substrate.message.response.BlockchainSubstrateResponse
import it.airgap.blockchain.substrate.message.response.PermissionSubstrateResponse
import it.airgap.blockchain.substrate.message.response.SignSubstrateResponse
import it.airgap.blockchain.substrate.message.response.TransferSubstrateResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
internal data class PermissionV3SubstrateRequest(
    val appMetadata: SubstrateAppMetadata,
    val scopes: List<SubstratePermission.Scope>,
    val networks: List<SubstrateNetwork>?,
) : PermissionV3BeaconRequestContent.BlockchainData() {
    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        blockchainIdentifier: String,
    ): BeaconMessage = PermissionSubstrateRequest(id, version, blockchainIdentifier, senderId, origin, appMetadata, scopes, networks ?: emptyList())

    companion object {
        fun from(permissionRequest: PermissionSubstrateRequest): PermissionV3SubstrateRequest = with(permissionRequest) {
            PermissionV3SubstrateRequest(
                appMetadata,
                scopes,
                if (networks.isNotEmpty()) networks else null,
            )
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator(BlockchainV3SubstrateRequest.CLASS_DISCRIMINATOR)
internal sealed class BlockchainV3SubstrateRequest : BlockchainV3BeaconRequestContent.BlockchainData() {
    abstract val scope: SubstratePermission.Scope

    companion object {
        const val CLASS_DISCRIMINATOR = "type"

        fun from(blockchainRequest: BlockchainSubstrateRequest): BlockchainV3SubstrateRequest = with(blockchainRequest) {
            when (this) {
                is TransferSubstrateRequest -> TransferV3SubstrateRequest(
                    scope,
                    sourceAddress,
                    amount,
                    recipient,
                    network,
                    TransferV3SubstrateRequest.Mode.from(mode),
                )
                is SignSubstrateRequest -> SignV3SubstrateRequest(
                    scope,
                    accountId,
                    network,
                    runtimeSpec,
                    payload,
                    SignV3SubstrateRequest.Mode.from(mode),
                )
            }
        }
    }
}

@Serializable
@SerialName(TransferV3SubstrateRequest.TYPE)
internal data class TransferV3SubstrateRequest(
    override val scope: SubstratePermission.Scope,
    val sourceAddress: String,
    val amount: String,
    val recipient: String,
    val network: SubstrateNetwork,
    val mode: Mode
) : BlockchainV3SubstrateRequest() {
    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        accountId: String,
        blockchainIdentifier: String,
    ): BeaconMessage {
        val appMetadata = dependencyRegistry.storageManager.findInstanceAppMetadata<SubstrateAppMetadata> { it.senderId == senderId }
        return TransferSubstrateRequest(
            id,
            version,
            blockchainIdentifier,
            senderId,
            appMetadata,
            origin,
            accountId,
            scope,
            sourceAddress,
            amount,
            recipient,
            network,
            mode.toTransferSubstrateRequestMode(),
        )
    }

    @Serializable
    enum class Mode {
        @SerialName("broadcast") Broadcast {
            override fun toTransferSubstrateRequestMode(): TransferSubstrateRequest.Mode = TransferSubstrateRequest.Mode.Broadcast
        },
        @SerialName("broadcast-and-return") BroadcastAndReturn {
            override fun toTransferSubstrateRequestMode(): TransferSubstrateRequest.Mode = TransferSubstrateRequest.Mode.BroadcastAndReturn
        },
        @SerialName("return") Return {
            override fun toTransferSubstrateRequestMode(): TransferSubstrateRequest.Mode = TransferSubstrateRequest.Mode.Return
        };
        
        abstract fun toTransferSubstrateRequestMode(): TransferSubstrateRequest.Mode
        
        companion object {
            fun from(transferMode: TransferSubstrateRequest.Mode): Mode = when (transferMode) {
                TransferSubstrateRequest.Mode.Broadcast -> Broadcast
                TransferSubstrateRequest.Mode.BroadcastAndReturn -> BroadcastAndReturn
                TransferSubstrateRequest.Mode.Return -> Return
            }
        }
    }

    companion object {
        const val TYPE = "transfer_request"
    }
}

@Serializable
@SerialName(TransferV3SubstrateRequest.TYPE)
internal data class SignV3SubstrateRequest(
    override val scope: SubstratePermission.Scope,
    val accountId: String,
    val network: SubstrateNetwork,
    val runtimeSpec: SubstrateRuntimeSpec,
    val payload: String,
    val mode: Mode
) : BlockchainV3SubstrateRequest() {
    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        accountId: String,
        blockchainIdentifier: String,
    ): BeaconMessage {
        val appMetadata = dependencyRegistry.storageManager.findInstanceAppMetadata<SubstrateAppMetadata> { it.senderId == senderId }
        return SignSubstrateRequest(
            id,
            version,
            blockchainIdentifier,
            senderId,
            appMetadata,
            origin,
            accountId,
            scope,
            network,
            runtimeSpec,
            payload,
            mode.toSignSubstrateRequestMode(),
        )
    }

    @Serializable
    enum class Mode {
        @SerialName("broadcast") Broadcast {
            override fun toSignSubstrateRequestMode(): SignSubstrateRequest.Mode = SignSubstrateRequest.Mode.Broadcast
        },
        @SerialName("broadcast-and-return") BroadcastAndReturn {
            override fun toSignSubstrateRequestMode(): SignSubstrateRequest.Mode = SignSubstrateRequest.Mode.BroadcastAndReturn
        },
        @SerialName("return") Return {
            override fun toSignSubstrateRequestMode(): SignSubstrateRequest.Mode = SignSubstrateRequest.Mode.Return
        };

        abstract fun toSignSubstrateRequestMode(): SignSubstrateRequest.Mode

        companion object {
            fun from(SignMode: SignSubstrateRequest.Mode): Mode = when (SignMode) {
                SignSubstrateRequest.Mode.Broadcast -> Broadcast
                SignSubstrateRequest.Mode.BroadcastAndReturn -> BroadcastAndReturn
                SignSubstrateRequest.Mode.Return -> Return
            }
        }
    }

    companion object {
        const val TYPE = "sign_request"
    }
}

@Serializable
internal data class PermissionV3SubstrateResponse(
    val appMetadata: SubstrateAppMetadata,
    val scopes: List<SubstratePermission.Scope>,
    val accounts: List<SubstrateAccount>,
) : PermissionV3BeaconResponseContent.BlockchainData() {
    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        accountId: String,
        blockchainIdentifier: String,
    ): BeaconMessage = PermissionSubstrateResponse(id, version, origin, blockchainIdentifier, accountId, appMetadata, scopes, accounts)

    companion object {
        fun from(requestResponse: PermissionSubstrateResponse): PermissionV3SubstrateResponse = with(requestResponse) {
            PermissionV3SubstrateResponse(
                appMetadata,
                scopes,
                accounts,
            )
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator(BlockchainV3SubstrateResponse.CLASS_DISCRIMINATOR)
internal sealed class BlockchainV3SubstrateResponse : BlockchainV3BeaconResponseContent.BlockchainData() {
    companion object {
        const val CLASS_DISCRIMINATOR = "type"

        fun from(blockchainResponse: BlockchainSubstrateResponse): BlockchainV3SubstrateResponse = with(blockchainResponse) {
            when (this) {
                is TransferSubstrateResponse -> TransferV3SubstrateResponse.from(this)
                is SignSubstrateResponse -> SignV3SubstrateResponse.from(this)
            }
        }
    }
}

@Serializable
@SerialName(TransferV3SubstrateResponse.TYPE)
internal data class TransferV3SubstrateResponse(
    val transactionHash: String? = null,
    val payload: String? = null,
) : BlockchainV3SubstrateResponse() {
    
    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        blockchainIdentifier: String,
    ): BeaconMessage = when {
        transactionHash != null && payload == null -> TransferSubstrateResponse.Broadcast(id, version, origin, blockchainIdentifier, transactionHash)
        transactionHash != null && payload != null -> TransferSubstrateResponse.BroadcastAndReturn(id, version, origin, blockchainIdentifier, transactionHash, payload)
        transactionHash == null && payload != null -> TransferSubstrateResponse.Return(id, version, origin, blockchainIdentifier, payload)
        else -> failWithInvalidMessage(this)
    }

    companion object {
        const val TYPE = "transfer_response"
        
        fun from(transferResponse: TransferSubstrateResponse): TransferV3SubstrateResponse = with(transferResponse) {
            when (this) {
                is TransferSubstrateResponse.Broadcast -> TransferV3SubstrateResponse(transactionHash = transactionHash)
                is TransferSubstrateResponse.BroadcastAndReturn -> TransferV3SubstrateResponse(transactionHash, payload)
                is TransferSubstrateResponse.Return -> TransferV3SubstrateResponse(payload = payload)
            }
        }
    }
    
    private fun failWithInvalidMessage(message: TransferV3SubstrateResponse): Nothing = failWithIllegalArgument("Message $message is invalid.")
}

@Serializable
@SerialName(SignV3SubstrateResponse.TYPE)
internal data class SignV3SubstrateResponse(
    val signature: String? = null,
    val payload: String? = null,
) : BlockchainV3SubstrateResponse() {

    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        blockchainIdentifier: String,
    ): BeaconMessage = when {
        signature != null && payload == null -> SignSubstrateResponse.Broadcast(id, version, origin, blockchainIdentifier, signature)
        signature != null && payload != null -> SignSubstrateResponse.BroadcastAndReturn(id, version, origin, blockchainIdentifier, signature, payload)
        signature == null && payload != null -> SignSubstrateResponse.Return(id, version, origin, blockchainIdentifier, payload)
        else -> failWithInvalidMessage(this)
    }

    companion object {
        const val TYPE = "Sign_response"

        fun from(signResponse: SignSubstrateResponse): SignV3SubstrateResponse = with(signResponse) {
            when (this) {
                is SignSubstrateResponse.Broadcast -> SignV3SubstrateResponse(signature = signature)
                is SignSubstrateResponse.BroadcastAndReturn -> SignV3SubstrateResponse(signature, payload)
                is SignSubstrateResponse.Return -> SignV3SubstrateResponse(payload = payload)
            }
        }
    }

    private fun failWithInvalidMessage(message: SignV3SubstrateResponse): Nothing = failWithIllegalArgument("Message $message is invalid.")
}