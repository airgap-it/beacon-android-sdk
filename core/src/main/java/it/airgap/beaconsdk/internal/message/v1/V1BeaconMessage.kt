package it.airgap.beaconsdk.internal.message.v1

import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.Network
import it.airgap.beaconsdk.data.beacon.PermissionScope
import it.airgap.beaconsdk.data.beacon.Threshold
import it.airgap.beaconsdk.data.tezos.TezosOperation
import it.airgap.beaconsdk.exception.BeaconException
import it.airgap.beaconsdk.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.internal.storage.decorator.DecoratedExtendedStorage
import it.airgap.beaconsdk.message.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed class V1BeaconMessage : VersionedBeaconMessage() {
    abstract val id: String
    abstract val beaconId: String

    override fun pairsWith(other: BeaconMessage): Boolean = other.id == id
    override fun pairsWith(other: VersionedBeaconMessage): Boolean =
        other is V1BeaconMessage && other.id == id

    override fun comesFrom(appMetadata: AppMetadata): Boolean = appMetadata.senderId == beaconId

    companion object {
        fun fromBeaconMessage(
            version: String,
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

                    is DisconnectBeaconMessage ->
                        DisconnectV1BeaconMessage(version, id, senderId)

                    is ErrorBeaconMessage ->
                        ErrorV1BeaconMessage(version, id, senderId, errorType)
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
    val scopes: List<PermissionScope>,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(storage: DecoratedExtendedStorage): BeaconMessage =
        PermissionBeaconRequest(id, beaconId, appMetadata.toAppMetadata(), network, scopes)
}

@Serializable
@SerialName("operation_request")
internal data class OperationV1BeaconRequest(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val network: Network,
    val operationDetails: TezosOperation,
    val sourceAddress: String,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(storage: DecoratedExtendedStorage): BeaconMessage {
        val appMetadata = storage.findAppMetadata { it.senderId == beaconId }
        return OperationBeaconRequest(id, beaconId, appMetadata, network, operationDetails, sourceAddress)
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
    override suspend fun toBeaconMessage(storage: DecoratedExtendedStorage): BeaconMessage {
        val appMetadata = storage.findAppMetadata { it.senderId == beaconId }
        return SignPayloadBeaconRequest(id, beaconId, appMetadata, payload, sourceAddress)
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
    override suspend fun toBeaconMessage(storage: DecoratedExtendedStorage): BeaconMessage {
        val appMetadata = storage.findAppMetadata { it.senderId == beaconId }
        return BroadcastBeaconRequest(id, beaconId, appMetadata, network, signedTransaction)
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
    val scopes: List<PermissionScope>,
    val threshold: Threshold? = null,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(storage: DecoratedExtendedStorage): BeaconMessage =
        PermissionBeaconResponse(id, publicKey, network, scopes, threshold)
}

@Serializable
@SerialName("operation_response")
internal data class OperationV1BeaconResponse(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val transactionHash: String,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(storage: DecoratedExtendedStorage): BeaconMessage =
        OperationBeaconResponse(id, transactionHash)
}

@Serializable
@SerialName("sign_payload_response")
internal data class SignPayloadV1BeaconResponse(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val signature: String,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(storage: DecoratedExtendedStorage): BeaconMessage =
        SignPayloadBeaconResponse(id, signature)
}

@Serializable
@SerialName("broadcast_response")
internal data class BroadcastV1BeaconResponse(
    override val version: String,
    override val id: String,
    override val beaconId: String,
    val transactionHash: String,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(storage: DecoratedExtendedStorage): BeaconMessage =
        BroadcastBeaconResponse(id, transactionHash)
}

@Serializable
@SerialName("disconnect")
internal data class DisconnectV1BeaconMessage(
    override val version: String,
    override val id: String,
    override var beaconId: String,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(storage: DecoratedExtendedStorage): BeaconMessage =
        DisconnectBeaconMessage(id, beaconId)
}

@Serializable
@SerialName("error")
internal data class ErrorV1BeaconMessage(
    override val version: String,
    override val id: String,
    override var beaconId: String,
    val errorType: BeaconException.Type,
) : V1BeaconMessage() {
    override suspend fun toBeaconMessage(storage: DecoratedExtendedStorage): BeaconMessage =
        ErrorBeaconMessage(id, beaconId, errorType)
}