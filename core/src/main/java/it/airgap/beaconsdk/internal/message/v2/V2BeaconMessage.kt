package it.airgap.beaconsdk.internal.message.v2

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
internal sealed class V2BeaconMessage : VersionedBeaconMessage() {
    abstract val id: String
    abstract val senderId: String

    override fun pairsWith(other: BeaconMessage): Boolean = other.id == id
    override fun pairsWith(other: VersionedBeaconMessage): Boolean =
        other is V2BeaconMessage && other.id == id

    override fun comesFrom(appMetadata: AppMetadata): Boolean = appMetadata.senderId == senderId

    companion object {
        fun fromBeaconMessage(
            version: String,
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
                        SignPayloadV2BeaconRequest(version, id, senderId, payload, sourceAddress)

                    is BroadcastBeaconRequest ->
                        BroadcastV2BeaconRequest(version, id, senderId, network, signedTransaction)

                    is PermissionBeaconResponse ->
                        PermissionV2BeaconResponse(version, id, senderId, publicKey, network, scopes, threshold)

                    is OperationBeaconResponse ->
                        OperationV2BeaconResponse(version, id, senderId, transactionHash)

                    is SignPayloadBeaconResponse ->
                        SignPayloadV2BeaconResponse(version, id, senderId, signature)

                    is BroadcastBeaconResponse ->
                        BroadcastV2BeaconResponse(version, id, senderId, transactionHash)

                    is DisconnectBeaconMessage ->
                        DisconnectV2BeaconMessage(version, id, senderId)

                    is ErrorBeaconMessage ->
                        ErrorV2BeaconMessage(version, id, senderId, errorType)
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
    val scopes: List<PermissionScope>,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(storage: DecoratedExtendedStorage): BeaconMessage =
        PermissionBeaconRequest(id, senderId, appMetadata.toAppMetadata(), network, scopes)
}

@Serializable
@SerialName("operation_request")
internal data class OperationV2BeaconRequest(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val network: Network,
    val operationDetails: TezosOperation,
    val sourceAddress: String,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(storage: DecoratedExtendedStorage): BeaconMessage {
        val appMetadata = storage.findAppMetadata { it.senderId == senderId }
        return OperationBeaconRequest(id, senderId, appMetadata, network, operationDetails, sourceAddress)
    }
}

@Serializable
@SerialName("sign_payload_request")
internal data class SignPayloadV2BeaconRequest(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val payload: String,
    val sourceAddress: String,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(storage: DecoratedExtendedStorage): BeaconMessage {
        val appMetadata = storage.findAppMetadata { it.senderId == senderId }
        return SignPayloadBeaconRequest(id, senderId, appMetadata, payload, sourceAddress)
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
    override suspend fun toBeaconMessage(storage: DecoratedExtendedStorage): BeaconMessage {
        val appMetadata = storage.findAppMetadata { it.senderId == senderId }
        return BroadcastBeaconRequest(id, senderId, appMetadata, network, signedTransaction)
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
    val scopes: List<PermissionScope>,
    val threshold: Threshold? = null,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(storage: DecoratedExtendedStorage): BeaconMessage =
        PermissionBeaconResponse(id, publicKey, network, scopes, threshold)
}

@Serializable
@SerialName("operation_response")
internal data class OperationV2BeaconResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val transactionHash: String,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(storage: DecoratedExtendedStorage): BeaconMessage =
        OperationBeaconResponse(id, transactionHash)
}

@Serializable
@SerialName("sign_payload_response")
internal data class SignPayloadV2BeaconResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val signature: String,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(storage: DecoratedExtendedStorage): BeaconMessage =
        SignPayloadBeaconResponse(id, signature)
}

@Serializable
@SerialName("broadcast_response")
internal data class BroadcastV2BeaconResponse(
    override val version: String,
    override val id: String,
    override val senderId: String,
    val transactionHash: String,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(storage: DecoratedExtendedStorage): BeaconMessage =
        BroadcastBeaconResponse(id, transactionHash)
}

@Serializable
@SerialName("disconnect")
internal data class DisconnectV2BeaconMessage(
    override val version: String,
    override val id: String,
    override var senderId: String,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(storage: DecoratedExtendedStorage): BeaconMessage =
        DisconnectBeaconMessage(id, senderId)
}

@Serializable
@SerialName("error")
internal data class ErrorV2BeaconMessage(
    override val version: String,
    override val id: String,
    override var senderId: String,
    val errorType: BeaconException.Type,
) : V2BeaconMessage() {
    override suspend fun toBeaconMessage(storage: DecoratedExtendedStorage): BeaconMessage =
        ErrorBeaconMessage(id, senderId, errorType)
}