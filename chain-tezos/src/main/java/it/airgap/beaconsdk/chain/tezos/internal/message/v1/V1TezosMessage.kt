package it.airgap.beaconsdk.chain.tezos.internal.message.v1

import it.airgap.beaconsdk.chain.tezos.Tezos
import it.airgap.beaconsdk.chain.tezos.data.operation.TezosOperation
import it.airgap.beaconsdk.chain.tezos.internal.utils.failWithUnknownMessage
import it.airgap.beaconsdk.chain.tezos.internal.utils.failWithUnknownPayload
import it.airgap.beaconsdk.chain.tezos.message.*
import it.airgap.beaconsdk.core.data.Network
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.data.SigningType
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.ChainBeaconRequest
import it.airgap.beaconsdk.core.message.ChainBeaconResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal sealed class V1TezosMessage : V1BeaconMessage() {

    companion object : Factory<BeaconMessage, V1BeaconMessage> {
        override fun from(senderId: String, content: BeaconMessage): V1TezosMessage =
            when (content) {
                is ChainBeaconRequest -> when (val payload = content.payload) {
                    is OperationTezosRequest -> OperationV1TezosRequest(
                        content.version,
                        content.id,
                        content.senderId,
                        payload.network,
                        payload.operationDetails,
                        payload.sourceAddress,
                    )
                    is SignPayloadTezosRequest -> SignPayloadV1TezosRequest(
                        content.version,
                        content.id,
                        content.senderId,
                        payload.payload,
                        payload.sourceAddress,
                    )
                    is BroadcastTezosRequest -> BroadcastV1TezosRequest(
                        content.version,
                        content.id,
                        content.senderId,
                        payload.network,
                        payload.signedTransaction,
                    )
                    else -> failWithUnknownPayload(payload)
                }
                is ChainBeaconResponse -> when (val payload = content.payload) {
                    is OperationTezosResponse -> OperationV1TezosResponse(
                        content.version,
                        content.id,
                        senderId,
                        payload.transactionHash,
                    )
                    is SignPayloadTezosResponse -> SignPayloadV1TezosResponse(
                        content.version,
                        content.id,
                        senderId,
                        payload.signature,
                    )
                    is BroadcastTezosResponse -> BroadcastV1TezosResponse(
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
@SerialName(OperationV1TezosRequest.TYPE)
internal data class OperationV1TezosRequest(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val network: Network,
    val operationDetails: List<TezosOperation>,
    val sourceAddress: String,
) : V1TezosMessage() {
    @Transient
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage {
        val appMetadata = storageManager.findAppMetadata { it.senderId == beaconId }
        return ChainBeaconRequest(
            id,
            beaconId,
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

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage {
        val appMetadata = storageManager.findAppMetadata { it.senderId == beaconId }
        return ChainBeaconRequest(
            id,
            beaconId,
            appMetadata,
            Tezos.IDENTIFIER,
            SignPayloadTezosRequest(
                SigningType.Raw,
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
@SerialName(BroadcastV1TezosRequest.TYPE)
internal data class BroadcastV1TezosRequest(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val network: Network,
    val signedTransaction: String,
) : V1TezosMessage() {
    @Transient
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage {
        val appMetadata = storageManager.findAppMetadata { it.senderId == beaconId }
        return ChainBeaconRequest(
            id,
            beaconId,
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
@SerialName(OperationV1TezosResponse.TYPE)
internal data class OperationV1TezosResponse(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val transactionHash: String,
) : V1TezosMessage() {
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
@SerialName(SignPayloadV1TezosResponse.TYPE)
internal data class SignPayloadV1TezosResponse(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val signature: String,
) : V1TezosMessage() {
    @Transient
    override val type: String = TYPE

    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        ChainBeaconResponse(
            id,
            Tezos.IDENTIFIER,
            SignPayloadTezosResponse(
                SigningType.Raw,
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
@SerialName(BroadcastV1TezosResponse.TYPE)
internal data class BroadcastV1TezosResponse(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val transactionHash: String,
) : V1TezosMessage() {
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
