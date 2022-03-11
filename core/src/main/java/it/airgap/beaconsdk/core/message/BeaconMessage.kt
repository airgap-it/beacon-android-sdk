package it.airgap.beaconsdk.core.message

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.data.Origin

/**
 * Base class for messages used in the Beacon communication.
 *
 * @property [id] A unique value used to identify the pair of request and response messages.
 */
public sealed class BeaconMessage {
    public abstract val id: String
    public abstract val version: String

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract val associatedOrigin: Origin

    public companion object {}
}

// -- request --

/**
 * Base class for request messages used in the Beacon communication.
 *
 * @property [blockchainIdentifier] The unique name of the blockchain that specifies the request.
 * @property [senderId] A unique value used to identify the sender of the request.
 * @property [appMetadata] An optional metadata describing the dApp that is the sender of the request.
 * @property [origin] An origination data used to identify the source of the request.
 */
public sealed class BeaconRequest : BeaconMessage() {
    public abstract val blockchainIdentifier: String
    public abstract val senderId: String
    public abstract val appMetadata: AppMetadata?

    public abstract val origin: Origin

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val associatedOrigin: Origin
        get() = origin

    public companion object {}
}

/**
 * Message requesting the granting of the specified permissions to the [sender dApp][appMetadata].
 *
 * Expects [PermissionBeaconResponse] as a response.
 *
 * @property [appMetadata] The metadata describing the dApp asking for permissions.
 */
public abstract class PermissionBeaconRequest : BeaconRequest() {
    public abstract override val appMetadata: AppMetadata

    public companion object {}
}


/**
 * Base for class for blockchain specific messages.
 *
 * Expects [BlockchainBeaconResponse] as a response.
 *
 * @property [accountId] The account identifier of the account that is requested to handle this request.
 */
public abstract class BlockchainBeaconRequest : BeaconRequest() {
    public abstract val accountId: String?

    public companion object {}
}

// -- response --

/**
 * Base class for response messages used in the Beacon communication.
 */
public sealed class BeaconResponse : BeaconMessage() {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract val requestOrigin: Origin

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val associatedOrigin: Origin
        get() = requestOrigin

    public companion object {}
}

/**
 * Message responding to [PermissionBeaconRequest].
 *
 * @property [id] The value that identifies the request to which the message is responding.
 * @property [blockchainIdentifier] The unique name of the blockchain that specifies the request.
 */
public abstract class PermissionBeaconResponse : BeaconResponse() {
    public abstract val blockchainIdentifier: String

    public companion object {}
}

/**
 * A message responding to [BlockchainBeaconRequest]
 *
 * @property [blockchainIdentifier] A unique name of the blockchain that specifies the request.
 */
public abstract class BlockchainBeaconResponse : BeaconResponse() {
    public abstract val blockchainIdentifier: String

    public companion object {}
}

/**
 * Message responding to every [BeaconRequest], sent to confirm receiving of the request.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class AcknowledgeBeaconResponse(
    override val id: String,
    override val version: String,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override val requestOrigin: Origin,
    val senderId: String,
) : BeaconResponse() {

    public companion object {

        /**
         * Creates a new instance of [AcknowledgeBeaconResponse] from the [request]
         * with the specified [senderId].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(request: BeaconRequest, senderId: String): AcknowledgeBeaconResponse =
            AcknowledgeBeaconResponse(request.id, request.version, request.origin, senderId)
    }
}

/**
 * Message responding to every [BeaconRequest] and informing that the request could not be completed due to an error.
 *
 * @property [id] The value that identifies the request to which the message is responding.
 * @property [errorType] The type of the error.
 * @property [description] Additional and optional details.
 */
public data class ErrorBeaconResponse @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor(
    override val id: String,
    override val version: String,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override val requestOrigin: Origin,
    public val errorType: BeaconError,
    public val description: String? = null,
) : BeaconResponse() {

    public companion object {

        /**
         * Creates a new instance of [ErrorBeaconResponse] from the [request]
         * with the specified [errorType].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(request: BeaconRequest, errorType: BeaconError.Aborted, description: String? = null): ErrorBeaconResponse {
            return ErrorBeaconResponse(request.id, request.version, request.origin, errorType, description)
        }

        /**
         * Creates a new instance of [ErrorBeaconResponse] from the [request]
         * with the specified [errorType].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(request: BeaconRequest, errorType: BeaconError.Unknown, description: String? = null): ErrorBeaconResponse {
            return ErrorBeaconResponse(request.id, request.version, request.origin, errorType, description)
        }
    }
}

// -- other --

/**
 * Message informing that its sender has closed the connection.
 *
 * @property [id] The value that identifies this message.
 * @property [senderId] The value that identifies the sender of this message.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class DisconnectBeaconMessage(
    override val id: String,
    val senderId: String,
    override val version: String,
    val origin: Origin,
) : BeaconMessage() {
    override val associatedOrigin: Origin
        get() = origin

    public companion object {}
}