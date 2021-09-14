package it.airgap.beaconsdk.chain.tezos.internal.message.v2

import it.airgap.beaconsdk.chain.tezos.Tezos
import it.airgap.beaconsdk.chain.tezos.data.operation.TezosOperation
import it.airgap.beaconsdk.chain.tezos.internal.utils.failWithUnknownMessage
import it.airgap.beaconsdk.chain.tezos.internal.utils.failWithUnknownPayload
import it.airgap.beaconsdk.chain.tezos.message.*
import it.airgap.beaconsdk.core.data.Network
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.data.SigningType
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.ChainBeaconRequest
import it.airgap.beaconsdk.core.message.ChainBeaconResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal sealed class V2TezosMessage : V2BeaconMessage() {

    companion object : Factory<BeaconMessage, V2BeaconMessage> {
        override fun from(senderId: String, content: BeaconMessage): V2TezosMessage =
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
}

@Serializable
@SerialName(OperationV2TezosRequest.TYPE)
internal data class OperationV2TezosRequest(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val network: Network,
    val operationDetails: List<TezosOperation>,
    val sourceAddress: String,
) : V2TezosMessage() {
    @Transient
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
@SerialName(BroadcastV2TezosRequest.TYPE)
internal data class BroadcastV2TezosRequest(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val network: Network,
    val signedTransaction: String,
) : V2TezosMessage() {
    @Transient
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
@SerialName(OperationV2TezosResponse.TYPE)
internal data class OperationV2TezosResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val transactionHash: String,
) : V2TezosMessage() {
    @Transient
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
@SerialName(BroadcastV2TezosResponse.TYPE)
internal data class BroadcastV2TezosResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val transactionHash: String,
) : V2TezosMessage() {
    @Transient
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
