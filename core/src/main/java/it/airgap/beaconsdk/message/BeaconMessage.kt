package it.airgap.beaconsdk.message

import it.airgap.beaconsdk.data.beacon.*
import it.airgap.beaconsdk.data.tezos.TezosOperation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Base class for messages used in the Beacon communication.
 *
 * @property [id] A unique value used to identify the pair of request and response messages.
 */
@Serializable
public sealed class BeaconMessage {
    public abstract val id: String

    internal abstract val version: String
    internal abstract val associatedOrigin: Origin

    public companion object {}
}

// -- request --

/**
 * Base class for request messages used in the Beacon communication.
 *
 * @property [senderId] A unique value used to identify the sender of the request.
 * @property [origin] An origination data used to identify the source of the request.
 */
@Serializable
@SerialName("request")
public sealed class BeaconRequest : BeaconMessage() {
    public abstract val senderId: String
    public abstract val origin: Origin

    override val associatedOrigin: Origin
        get() = origin

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
 * @property [origin] The origination data of this request.
 */
@Serializable
@SerialName("permission_request")
public data class PermissionBeaconRequest internal constructor(
    override val id: String,
    override val senderId: String,
    val appMetadata: AppMetadata,
    public val network: Network,
    public val scopes: List<Permission.Scope>,
    override val origin: Origin,
    override val version: String,
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
 * @property [origin] The origination data of this request.
 */
@Serializable
@SerialName("operation_request")
public data class OperationBeaconRequest internal constructor(
    override val id: String,
    override val senderId: String,
    val appMetadata: AppMetadata?,
    public val network: Network,
    public val operationDetails: List<TezosOperation>,
    public val sourceAddress: String,
    override val origin: Origin,
    override val version: String,
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
 * @property [signingType] The requested type of signature. The client MUST fail if cannot provide the specified signature.
 * @property [payload] The payload to be signed.
 * @property [sourceAddress] The address of the account with which the payload should be signed.
 * @property [origin] The origination data of this request.
 */
@Serializable
@SerialName("sign_payload_request")
public data class SignPayloadBeaconRequest internal constructor(
    override val id: String,
    override val senderId: String,
    val appMetadata: AppMetadata?,
    public val signingType: SigningType,
    public val payload: String,
    public val sourceAddress: String,
    override val origin: Origin,
    override val version: String,
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
 * @property [origin] The origination data of this request.
 */
@Serializable
@SerialName("broadcast_request")
public data class BroadcastBeaconRequest internal constructor(
    override val id: String,
    override val senderId: String,
    val appMetadata: AppMetadata?,
    public val network: Network,
    public val signedTransaction: String,
    override val origin: Origin,
    override val version: String,
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
    internal abstract val requestOrigin: Origin
    override val associatedOrigin: Origin
        get() = requestOrigin

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
public data class PermissionBeaconResponse internal constructor(
    override val id: String,
    public val publicKey: String,
    public val network: Network,
    public val scopes: List<Permission.Scope>,
    public val threshold: Threshold? = null,
    override val version: String,
    override val requestOrigin: Origin,
) : BeaconResponse() {

    public companion object {

        /**
         * Creates a new instance of [PermissionBeaconResponse] from the [request]
         * with the specified [publicKey] and optional [network], [scopes] and [threshold].
         *
         * The response will have an id matching the one of the [request].
         * If no custom [network] and [scopes] are provided, the values will be also taken from the [request].
         * By default [threshold] is set to `null`.
         */
        public fun from(
            request: PermissionBeaconRequest,
            publicKey: String,
            network: Network = request.network,
            scopes: List<Permission.Scope> = request.scopes,
            threshold: Threshold? = null,
        ): PermissionBeaconResponse =
            PermissionBeaconResponse(request.id, publicKey, network, scopes, threshold, request.version, request.origin)
    }
}

/**
 * Message responding to [OperationBeaconRequest].
 *
 * @property [id] The value that identifies the request to which the message is responding.
 * @property [transactionHash] The hash of the broadcast operations.
 */
@Serializable
@SerialName("operation_response")
public data class OperationBeaconResponse internal constructor(
    override val id: String,
    public val transactionHash: String,
    override val version: String,
    override val requestOrigin: Origin,
) : BeaconResponse() {

    public companion object {

        /**
         * Creates a new instance of [OperationBeaconResponse] from the [request]
         * with the specified [transactionHash].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(request: OperationBeaconRequest, transactionHash: String): OperationBeaconResponse =
            OperationBeaconResponse(request.id, transactionHash, request.version, request.origin)
    }
}

/**
 * Message responding to [SignPayloadBeaconRequest].
 *
 * @property [id] The value that identifies the request to which the message is responding.
 * @property [signingType] The signature type.
 * @property [signature] The payload signature.
 */
@Serializable
@SerialName("sign_payload_response")
public data class SignPayloadBeaconResponse internal constructor(
    override val id: String,
    public val signingType: SigningType,
    public val signature: String,
    override val version: String,
    override val requestOrigin: Origin,
) : BeaconResponse() {

    public companion object {

        /**
         * Creates a new instance of [SignPayloadBeaconResponse] from the [request]
         * with the specified [signingType] and [signature].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(request: SignPayloadBeaconRequest, signingType: SigningType, signature: String): SignPayloadBeaconResponse =
            SignPayloadBeaconResponse(request.id, signingType, signature, request.version, request.origin)
    }
}

/**
 * Message responding to [BroadcastBeaconRequest].
 *
 * @property [id] The value that identifies the request to which the message is responding.
 * @property [transactionHash] The hash of the broadcast transaction.
 */
@Serializable
@SerialName("broadcast_response")
public data class BroadcastBeaconResponse internal constructor(
    override val id: String,
    public val transactionHash: String,
    override val version: String,
    override val requestOrigin: Origin,
) : BeaconResponse() {

    public companion object {

        /**
         * Creates a new instance of [BroadcastBeaconResponse] from the [request]
         * with the specified [transactionHash].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(request: BroadcastBeaconRequest, transactionHash: String): BroadcastBeaconResponse =
            BroadcastBeaconResponse(request.id, transactionHash, request.version, request.origin)
    }
}

/**
 * Message responding to every [BeaconRequest], sent to confirm receiving of the request.
 */
@Serializable
@SerialName("acknowledge")
internal data class AcknowledgeBeaconResponse(
    override val id: String,
    val senderId: String,
    override val version: String,
    override val requestOrigin: Origin,
) : BeaconResponse() {

    companion object {

        /**
         * Creates a new instance of [AcknowledgeBeaconResponse] from the [request]
         * with the specified [senderId].
         *
         * The response will have an id matching the one of the [request].
         */
        fun from(request: BeaconRequest, senderId: String): AcknowledgeBeaconResponse =
            AcknowledgeBeaconResponse(request.id, senderId, request.version, request.origin)
    }
}

/**
 * Message responding to every [BeaconRequest] and informing that the request could not be completed due to an error.
 *
 * @property [id] The value that identifies the request to which the message is responding.
 * @property [errorType] The type of the error.
 */
@Serializable
@SerialName("error")
public data class ErrorBeaconResponse internal constructor(
    override val id: String,
    val errorType: BeaconError,
    override val version: String,
    override val requestOrigin: Origin,
) : BeaconResponse() {

    public companion object {

        /**
         * Creates a new instance of [ErrorBeaconResponse] from the [request]
         * with the specified [errorType].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(request: BeaconRequest, errorType: BeaconError): ErrorBeaconResponse =
            ErrorBeaconResponse(request.id, errorType, request.version, request.origin)
    }
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
internal data class DisconnectBeaconMessage(
    override val id: String,
    val senderId: String,
    override val version: String,
    val origin: Origin,
) : BeaconMessage() {

    override val associatedOrigin: Origin
        get() = origin
}