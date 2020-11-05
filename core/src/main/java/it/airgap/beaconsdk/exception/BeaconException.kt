package it.airgap.beaconsdk.exception

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

public sealed class BeaconException(
    internal val type: Type? = null,
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {

    public class BroadcastError(cause: Throwable? = null) : BeaconException(Type.BroadcastError, MESSAGE_BROADCAST_ERROR, cause)
    public class NetworkNotSupported(cause: Throwable? = null) : BeaconException(Type.NetworkNotSupported, MESSAGE_NETWORK_NOT_SUPPORTED, cause)
    public class NoAddressError(cause: Throwable? = null) : BeaconException(Type.NoAddressError, MESSAGE_NO_ADDRESS_ERROR, cause)
    public class NoPrivateKeyFound(cause: Throwable? = null) : BeaconException(Type.NoPrivateKeyFound, MESSAGE_NO_PRIVATE_KEY_FOUND, cause)
    public class NotGranted(cause: Throwable? = null) : BeaconException(Type.NotGranted, MESSAGE_NOT_GRANTED, cause)
    public class ParametersInvalid(cause: Throwable? = null) : BeaconException(Type.ParametersInvalid, MESSAGE_PARAMETERS_INVALID, cause)
    public class TooManyOperations(cause: Throwable? = null) : BeaconException(Type.TooManyOperations, MESSAGE_TOO_MANY_OPERATIONS, cause)
    public class TransactionInvalid(cause: Throwable? = null) : BeaconException(Type.TransactionInvalid, MESSAGE_TRANSACTION_INVALID, cause)
    public class Aborted(cause: Throwable? = null) : BeaconException(Type.Aborted, MESSAGE_ABORTED, cause)
    public class Unknown(message: String? = null, cause: Throwable? = null) : BeaconException(Type.Unknown, message ?: MESSAGE_UNKNOWN, cause)
    public class Internal(message: String? = null, cause: Throwable? = null) : BeaconException(message = message ?: MESSAGE_INTERNAL, cause = cause)

    public companion object {
        private const val MESSAGE_BROADCAST_ERROR = "The transaction could not be broadcast to the network"
        private const val MESSAGE_NETWORK_NOT_SUPPORTED = "The wallet does not support this network"
        private const val MESSAGE_NO_ADDRESS_ERROR = "The wallet does not have an account set up"
        private const val MESSAGE_NO_PRIVATE_KEY_FOUND = "The account is not available"
        private const val MESSAGE_NOT_GRANTED = "Can't perform this action, permissions are not granted"
        private const val MESSAGE_PARAMETERS_INVALID = "The request could not be completed, some of the parameters are invalid"
        private const val MESSAGE_TOO_MANY_OPERATIONS = "The request contains too many transactions"
        private const val MESSAGE_TRANSACTION_INVALID = "The transaction is invalid and the node did not accept it"
        private const val MESSAGE_ABORTED = "This action was aborted by the user"
        private const val MESSAGE_UNKNOWN = "Unknown error"
        private const val MESSAGE_INTERNAL = "Internal error"

        internal fun fromType(type: Type, cause: Throwable? = null): BeaconException =
            when (type) {
                Type.BroadcastError -> BroadcastError(cause)
                Type.NetworkNotSupported -> NetworkNotSupported(cause)
                Type.NoAddressError -> NoAddressError(cause)
                Type.NoPrivateKeyFound -> NoPrivateKeyFound(cause)
                Type.NotGranted -> NotGranted(cause)
                Type.ParametersInvalid -> ParametersInvalid(cause)
                Type.TooManyOperations -> TooManyOperations(cause)
                Type.TransactionInvalid -> TransactionInvalid(cause)
                Type.Aborted -> Aborted(cause)
                Type.Unknown -> Unknown(cause = cause)
            }
    }

    @Serializable
    internal enum class Type {
        @SerialName("BROADCAST_ERROR")
        BroadcastError,

        @SerialName("NETWORK_NOT_SUPPORTED_ERROR")
        NetworkNotSupported,

        @SerialName("NO_ADDRESS_ERROR")
        NoAddressError,

        @SerialName("NO_PRIVATE_KEY_FOUND_ERROR")
        NoPrivateKeyFound,

        @SerialName("NOT_GRANTED_ERROR")
        NotGranted,

        @SerialName("PARAMETERS_INVALID_TYPE")
        ParametersInvalid,

        @SerialName("TOO_MANY_OPERATIONS_TYPE")
        TooManyOperations,

        @SerialName("TRANSACTION_INVALID_TYPE")
        TransactionInvalid,

        @SerialName("ABORTED_ERROR")
        Aborted,

        @SerialName("UNKNOWN_ERROR")
        Unknown,
    }
}