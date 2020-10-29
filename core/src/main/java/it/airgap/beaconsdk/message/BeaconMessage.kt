package it.airgap.beaconsdk.message

import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.Network
import it.airgap.beaconsdk.data.beacon.PermissionScope
import it.airgap.beaconsdk.data.beacon.Threshold
import it.airgap.beaconsdk.data.tezos.TezosOperation
import it.airgap.beaconsdk.exception.BeaconException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public sealed class BeaconMessage {
    public abstract val id: String

    public companion object {}
}

// -- request --

@Serializable
@SerialName("request")
public sealed class BeaconRequest : BeaconMessage() {
    public abstract val senderId: String

    public companion object {}
}

@Serializable
@SerialName("permission_request")
public data class PermissionBeaconRequest internal constructor(
    override val id: String,
    override val senderId: String,
    val appMetadata: AppMetadata,
    public val network: Network,
    public val scopes: List<PermissionScope>,
) : BeaconRequest() {
    public companion object {}
}

@Serializable
@SerialName("operation_request")
public data class OperationBeaconRequest internal constructor(
    override val id: String,
    override val senderId: String,
    val appMetadata: AppMetadata?,
    public val network: Network,
    public val operationDetails: TezosOperation,
    public val sourceAddress: String,
) : BeaconRequest() {
    public companion object {}
}

@Serializable
@SerialName("sign_payload_request")
public data class SignPayloadBeaconRequest internal constructor(
    override val id: String,
    override val senderId: String,
    val appMetadata: AppMetadata?,
    public val payload: String,
    public val sourceAddress: String,
) : BeaconRequest() {
    public companion object {}
}

@Serializable
@SerialName("broadcast_request")
public data class BroadcastBeaconRequest internal constructor(
    override val id: String,
    override val senderId: String,
    val appMetadata: AppMetadata?,
    public val network: Network,
    public val signedTransaction: String,
) : BeaconRequest() {
    public companion object {}
}

// -- response --

@Serializable
@SerialName("response")
public sealed class BeaconResponse : BeaconMessage() {
    public companion object {}
}


@Serializable
@SerialName("permission_response")
public data class PermissionBeaconResponse(
    override val id: String,
    public val publicKey: String,
    public val network: Network,
    public val scopes: List<PermissionScope>,
    public val threshold: Threshold? = null,
) : BeaconResponse() {
    public companion object {}
}

@Serializable
@SerialName("operation_response")
public data class OperationBeaconResponse(
    override val id: String,
    public val transactionHash: String,
) : BeaconResponse() {
    public companion object {}
}

@Serializable
@SerialName("sign_payload_response")
public data class SignPayloadBeaconResponse(
    override val id: String,
    public val signature: String,
) : BeaconResponse() {
    public companion object {}
}

@Serializable
@SerialName("broadcast_response")
public data class BroadcastBeaconResponse(
    override val id: String,
    public val transactionHash: String,
) : BeaconResponse() {
    public companion object {}
}

// -- other --

@Serializable
@SerialName("disconnect")
internal data class DisconnectBeaconMessage internal constructor(
    override val id: String,
    val senderId: String,
) : BeaconMessage()

@Serializable
@SerialName("error")
internal data class ErrorBeaconMessage internal constructor(
    override val id: String,
    val senderId: String,
    val errorType: BeaconException.Type,
) : BeaconMessage()