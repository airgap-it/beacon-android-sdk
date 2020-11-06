package it.airgap.beaconsdk.message

import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.Network
import it.airgap.beaconsdk.data.beacon.PermissionScope
import it.airgap.beaconsdk.data.beacon.Threshold
import it.airgap.beaconsdk.data.tezos.TezosOperation
import it.airgap.beaconsdk.exception.BeaconException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Base class for messages used in the Beacon communication.
 *
 * @property [id] The unique value used to identify the pair of request and response messages.
 */
@Serializable
public sealed class BeaconMessage {
    public abstract val id: String

    public companion object {}
}

// -- request --

/**
 * Base class for request messages used in the Beacon communication.
 *
 * @property [senderId] The unique value used to identify the sender of the message.
 */
@Serializable
@SerialName("request")
public sealed class BeaconRequest : BeaconMessage() {
    public abstract val senderId: String

    public companion object {}
}

/**
 * Message requesting the granting of the specified [permissions][scopes] to the [sender dApp][appMetadata].
 *
 * Expects [PermissionBeaconResponse] as a response.
 *
 * @property [id] The value that identifies this request.
 * @property [senderId] The value that identifies the sender of this request.
 * @property [appMetadata] The metadata describing the dApp asking for permissions.
 * @property [network] The network to which the permissions apply.
 * @property [scopes] The list of permissions asked to be granted.
 */
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

/**
 * Message requesting the broadcast of the given [Tezos operations][operationDetails].
 * The operations may be only partially filled by the dApp and lack certain information.
 *
 * Expects [OperationBeaconResponse] as a response.
 *
 * @property [id] The value that identifies this request.
 * @property [senderId] The value that identifies the sender of this request.
 * @property [appMetadata] The metadata describing the dApp asking for the broadcast. May be `null` if the [senderId] is unknown.
 * @property [network] The network on which the operations should be broadcast.
 * @property [operationDetails] Tezos operations which should be broadcast.
 * @property [sourceAddress] The address of the Tezos account that is requested to broadcast the operations.
 */
@Serializable
@SerialName("operation_request")
public data class OperationBeaconRequest internal constructor(
    override val id: String,
    override val senderId: String,
    val appMetadata: AppMetadata?,
    public val network: Network,
    public val operationDetails: TezosOperation, // TODO: change to `List<TezosOperation>`
    public val sourceAddress: String,
) : BeaconRequest() {
    public companion object {}
}

/**
 * Message requesting the signature of the given [payload].
 *
 * Expects [SignPayloadBeaconResponse] as a response.
 *
 * @property [id] The value that identifies this request.
 * @property [senderId] The value that identifies the sender of this request.
 * @property [appMetadata] The metadata describing the dApp asking for the signature. May be `null` if the [senderId] is unknown.
 * @property [payload] The payload to be signed.
 * @property [sourceAddress] The address of the account with which the payload should be signed.
 */
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

/**
 * Message requesting the broadcast of the given [transaction][signedTransaction].
 *
 * Expects [BroadcastBeaconResponse] as a response.
 *
 * @property [id] The value that identifies this request.
 * @property [senderId] The value that identifies the sender of this request.
 * @property [appMetadata] The metadata describing the dApp asking for the broadcast. May be `null` if the [senderId] is unknown.
 * @property [network] The network on which the transaction should be broadcast.
 * @property [signedTransaction] The transaction to be broadcast.
 */
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

/**
 * Base class for response messages used in the Beacon communication.
 */
@Serializable
@SerialName("response")
public sealed class BeaconResponse : BeaconMessage() {
    public companion object {}
}

/**
 * Message responding to [PermissionBeaconRequest].
 *
 * @property [id] The value that identifies the request to which the message is responding.
 * @property [publicKey] The public key of the account that is granting the permissions.
 * @property [network] The network to which the permissions apply.
 * @property [scopes] The list of granted permissions.
 * @property [threshold] An optional threshold configuration.
 */
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

/**
 * Message responding to [OperationBeaconRequest].
 *
 * @property [id] The value that identifies the request to which the message is responding.
 * @property [transactionHash] The hash of the broadcast operations.
 */
@Serializable
@SerialName("operation_response")
public data class OperationBeaconResponse(
    override val id: String,
    public val transactionHash: String,
) : BeaconResponse() {
    public companion object {}
}

/**
 * Message responding to [SignPayloadBeaconRequest].
 *
 * @property [id] The value that identifies the request to which the message is responding.
 * @property [signature] The payload signature.
 */
@Serializable
@SerialName("sign_payload_response")
public data class SignPayloadBeaconResponse(
    override val id: String,
    public val signature: String,
) : BeaconResponse() {
    public companion object {}
}

/**
 * Message responding to [BroadcastBeaconRequest].
 *
 * @property [id] The value that identifies the request to which the message is responding.
 * @property [transactionHash] The hash of the broadcast transaction.
 */
@Serializable
@SerialName("broadcast_response")
public data class BroadcastBeaconResponse(
    override val id: String,
    public val transactionHash: String,
) : BeaconResponse() {
    public companion object {}
}

// -- other --

/**
 * Message informing that its sender has closed the connection.
 *
 * @property [id] The value that identifies this message.
 * @property [senderId] The value that identifies the sender of this message.
 */
@Serializable
@SerialName("disconnect")
internal data class DisconnectBeaconMessage internal constructor(
    override val id: String,
    val senderId: String,
) : BeaconMessage()

/**
 * Message responding to every [BeaconRequest] informing that the request could not be completed due to an error.
 *
 * @property [id] The value that identifies the request to which the message is responding.
 * @property [errorType] The type of the error.
 */
// TODO: move to responses and remove `senderId`
@Serializable
@SerialName("error")
internal data class ErrorBeaconMessage internal constructor(
    override val id: String,
    val senderId: String,
    val errorType: BeaconException.Type,
) : BeaconMessage()