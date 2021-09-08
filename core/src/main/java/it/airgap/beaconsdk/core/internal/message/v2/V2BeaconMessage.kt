package it.airgap.beaconsdk.core.internal.message.v2

import it.airgap.beaconsdk.core.data.beacon.*
import it.airgap.beaconsdk.core.data.tezos.TezosOperation
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.message.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed class V2BeaconMessage : VersionedBeaconMessage() {
    abstract val id: String
    abstract val senderId: String

    companion object {
        fun fromBeaconMessage(
            senderId: String,
            message: BeaconMessage,
        ): V2BeaconMessage =
            with(message) {
                when (this) {
                    is PermissionBeaconRequest ->
                        PermissionV2BeaconRequest(version, id, senderId, V2AppMetadata.fromAppMetadata(appMetadata), network, scopes)

                    is OperationBeaconRequest ->
                        OperationV2BeaconRequest(version, id, senderId, network, operationDetails, sourceAddress)

                    is SignPayloadBeaconRequest ->
                        SignPayloadV2BeaconRequest(version, id, senderId, signingType, payload, sourceAddress)

                    is BroadcastBeaconRequest ->
                        BroadcastV2BeaconRequest(version, id, senderId, network, signedTransaction)

                    is PermissionBeaconResponse ->
                        PermissionV2BeaconResponse(version, id, senderId, publicKey, network, scopes, threshold)

                    is OperationBeaconResponse ->
                        OperationV2BeaconResponse(version, id, senderId, transactionHash)

                    is SignPayloadBeaconResponse ->
                        SignPayloadV2BeaconResponse(version, id, senderId, signingType, signature)

                    is BroadcastBeaconResponse ->
                        BroadcastV2BeaconResponse(version, id, senderId, transactionHash)

                    is AcknowledgeBeaconResponse ->
                        AcknowledgeV2BeaconResponse(version, id, senderId)

                    is ErrorBeaconResponse ->
                        ErrorV2BeaconResponse(version, id, senderId, errorType)

                    is DisconnectBeaconMessage ->
                        DisconnectV2BeaconMessage(version, id, senderId)
                }
            }
    }
}

@Serializable
@SerialName("permission_request")
internal data class PermissionV2BeaconRequest(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val appMetadata: V2AppMetadata,
    val network: Network,
    val scopes: List<Permission.Scope>,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        PermissionBeaconRequest(id, senderId, appMetadata.toAppMetadata(), network, scopes, origin, version)
}

@Serializable
@SerialName("operation_request")
internal data class OperationV2BeaconRequest(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val network: Network,
    val operationDetails: List<TezosOperation>,
    val sourceAddress: String,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage {
        val appMetadata = storageManager.findAppMetadata { it.senderId == senderId }
        return OperationBeaconRequest(id, senderId, appMetadata, network, operationDetails, sourceAddress, origin, version)
    }
}

@Serializable
@SerialName("sign_payload_request")
internal data class SignPayloadV2BeaconRequest(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val signingType: SigningType,
    val payload: String,
    val sourceAddress: String,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage {
        val appMetadata = storageManager.findAppMetadata { it.senderId == senderId }
        return SignPayloadBeaconRequest(id, senderId, appMetadata, signingType, payload, sourceAddress, origin, version)
    }
}

@Serializable
@SerialName("broadcast_request")
internal data class BroadcastV2BeaconRequest(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val network: Network,
    val signedTransaction: String,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage {
        val appMetadata = storageManager.findAppMetadata { it.senderId == senderId }
        return BroadcastBeaconRequest(id, senderId, appMetadata, network, signedTransaction, origin, version)
    }
}

@Serializable
@SerialName("permission_response")
internal data class PermissionV2BeaconResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val publicKey: String,
    val network: Network,
    val scopes: List<Permission.Scope>,
    val threshold: Threshold? = null,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        PermissionBeaconResponse(id, publicKey, network, scopes, threshold, version, origin)
}

@Serializable
@SerialName("operation_response")
internal data class OperationV2BeaconResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val transactionHash: String,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        OperationBeaconResponse(id, transactionHash, version, origin)
}

@Serializable
@SerialName("sign_payload_response")
internal data class SignPayloadV2BeaconResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val signingType: SigningType,
    val signature: String,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        SignPayloadBeaconResponse(id, signingType, signature, version, origin)
}

@Serializable
@SerialName("broadcast_response")
internal data class BroadcastV2BeaconResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val transactionHash: String,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        BroadcastBeaconResponse(id, transactionHash, version, origin)
}

@Serializable
@SerialName("acknowledge")
internal data class AcknowledgeV2BeaconResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        AcknowledgeBeaconResponse(id, senderId, version, origin)
}

@Serializable
@SerialName("error")
internal data class ErrorV2BeaconResponse(
    override val version: String,
    override val id: String,
    override var senderId: String,
    val errorType: BeaconError,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        ErrorBeaconResponse(id, errorType, version, origin)
}

@Serializable
@SerialName("disconnect")
internal data class DisconnectV2BeaconMessage(
    override val version: String,
    override val id: String,
    override var senderId: String,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        DisconnectBeaconMessage(id, senderId, version, origin)
}