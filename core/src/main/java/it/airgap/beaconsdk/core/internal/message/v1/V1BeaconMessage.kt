package it.airgap.beaconsdk.core.internal.message.v1

import it.airgap.beaconsdk.core.data.beacon.*
import it.airgap.beaconsdk.core.data.tezos.TezosOperation
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.failWithUnsupportedMessage
import it.airgap.beaconsdk.core.message.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed class V1BeaconMessage : VersionedBeaconMessage() {
    abstract val id: String
    abstract val beaconId: String

    companion object {
        fun fromBeaconMessage(
            senderId: String,
            message: BeaconMessage,
        ): V1BeaconMessage =
            with(message) {
                when (this) {
                    is PermissionBeaconRequest ->
                        PermissionV1BeaconRequest(version, id, this.senderId, V1AppMetadata.fromAppMetadata(appMetadata), network, scopes)

                    is OperationBeaconRequest ->
                        OperationV1BeaconRequest(version, id, this.senderId, network, operationDetails, sourceAddress)

                    is SignPayloadBeaconRequest ->
                        SignPayloadV1BeaconRequest(version, id, this.senderId, payload, sourceAddress)

                    is BroadcastBeaconRequest ->
                        BroadcastV1BeaconRequest(version, id, this.senderId, network, signedTransaction)

                    is PermissionBeaconResponse ->
                        PermissionV1BeaconResponse(version, id, senderId, publicKey, network, scopes, threshold)

                    is OperationBeaconResponse ->
                        OperationV1BeaconResponse(version, id, senderId, transactionHash)

                    is SignPayloadBeaconResponse ->
                        SignPayloadV1BeaconResponse(version, id, senderId, signature)

                    is BroadcastBeaconResponse ->
                        BroadcastV1BeaconResponse(version, id, senderId, transactionHash)

                    is AcknowledgeBeaconResponse ->
                        failWithUnsupportedMessage(message, version)

                    is ErrorBeaconResponse ->
                        ErrorV1BeaconResponse(version, id, senderId, errorType)

                    is DisconnectBeaconMessage ->
                        DisconnectV1BeaconMessage(version, id, senderId)
                }
            }
    }
}

@Serializable
@SerialName("permission_request")
internal data class PermissionV1BeaconRequest(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val appMetadata: V1AppMetadata,
    val network: Network,
    val scopes: List<Permission.Scope>,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        PermissionBeaconRequest(id, beaconId, appMetadata.toAppMetadata(), network, scopes, origin, version)
}

@Serializable
@SerialName("operation_request")
internal data class OperationV1BeaconRequest(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val network: Network,
    val operationDetails: List<TezosOperation>,
    val sourceAddress: String,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage {
        val appMetadata = storageManager.findAppMetadata { it.senderId == beaconId }
        return OperationBeaconRequest(id, beaconId, appMetadata, network, operationDetails, sourceAddress, origin, version)
    }
}

@Serializable
@SerialName("sign_payload_request")
internal data class SignPayloadV1BeaconRequest(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val payload: String,
    val sourceAddress: String,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage {
        val appMetadata = storageManager.findAppMetadata { it.senderId == beaconId }
        return SignPayloadBeaconRequest(id, beaconId, appMetadata, SigningType.Raw, payload, sourceAddress, origin, version)
    }
}

@Serializable
@SerialName("broadcast_request")
internal data class BroadcastV1BeaconRequest(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val network: Network,
    val signedTransaction: String,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage {
        val appMetadata = storageManager.findAppMetadata { it.senderId == beaconId }
        return BroadcastBeaconRequest(id, beaconId, appMetadata, network, signedTransaction, origin, version)
    }
}

@Serializable
@SerialName("permission_response")
internal data class PermissionV1BeaconResponse(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val publicKey: String,
    val network: Network,
    val scopes: List<Permission.Scope>,
    val threshold: Threshold? = null,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        PermissionBeaconResponse(id, publicKey, network, scopes, threshold, version, origin)
}

@Serializable
@SerialName("operation_response")
internal data class OperationV1BeaconResponse(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val transactionHash: String,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        OperationBeaconResponse(id, transactionHash, version, origin)
}

@Serializable
@SerialName("sign_payload_response")
internal data class SignPayloadV1BeaconResponse(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val signature: String,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        SignPayloadBeaconResponse(id, SigningType.Raw, signature, version, origin)
}

@Serializable
@SerialName("broadcast_response")
internal data class BroadcastV1BeaconResponse(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val transactionHash: String,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        BroadcastBeaconResponse(id, transactionHash, version, origin)
}

@Serializable
@SerialName("error")
internal data class ErrorV1BeaconResponse(
    override val version: String,
    override val id: String,
    override var beaconId: String,
    val errorType: BeaconError,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        ErrorBeaconResponse(id, errorType, version, origin)
}

@Serializable
@SerialName("disconnect")
internal data class DisconnectV1BeaconMessage(
    override val version: String,
    override val id: String,
    override var beaconId: String,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(origin: Origin, storageManager: StorageManager): BeaconMessage =
        DisconnectBeaconMessage(id, beaconId, version, origin)
}