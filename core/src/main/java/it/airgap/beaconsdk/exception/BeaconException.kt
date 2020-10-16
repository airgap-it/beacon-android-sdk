package it.airgap.beaconsdk.exception

sealed class BeaconException(val type: Type, message: String? = null, cause: Throwable? = null) : Exception(message, cause) {

    class BroadcastError(cause: Throwable? = null) : BeaconException(Type.BroadcastError, MESSAGE_BROADCAST_ERROR, cause)
    class NetworkNotSupported(cause: Throwable? = null) : BeaconException(Type.NetworkNotSupported, MESSAGE_NETWORK_NOT_SUPPORTED, cause)
    class NoAddressError(cause: Throwable? = null) : BeaconException(Type.NoAddressError, MESSAGE_NO_ADDRESS_ERROR, cause)
    class NoPrivateKeyFound(cause: Throwable? = null) : BeaconException(Type.NoPrivateKeyFound, MESSAGE_NO_PRIVATE_KEY_FOUND, cause)
    class NotGranted(cause: Throwable? = null) : BeaconException(Type.NotGranted, MESSAGE_NOT_GRANTED, cause)
    class ParametersInvalid(cause: Throwable? = null) : BeaconException(Type.ParametersInvalid, MESSAGE_PARAMETERS_INVALID, cause)
    class TooManyOperations(cause: Throwable? = null) : BeaconException(Type.TooManyOperations, MESSAGE_TOO_MANY_OPERATIONS, cause)
    class TransactionInvalid(cause: Throwable? = null) : BeaconException(Type.TransactionInvalid, MESSAGE_TRANSACTION_INVALID, cause)
    class Aborted(cause: Throwable? = null) : BeaconException(Type.Aborted, MESSAGE_ABORTED, cause)
    class Unknown(message: String? = null, cause: Throwable? = null) : BeaconException(Type.Unknown, message ?: MESSAGE_UNKNOWN, cause)
    class Internal(cause: Throwable? = null) : BeaconException(Type.Internal, MESSAGE_INTERNAL, cause)

    companion object {
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

        fun fromType(type: Type, cause: Throwable? = null): BeaconException =
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
                Type.Internal -> Internal(cause)
            }
    }

    enum class Type {
        BroadcastError,
        NetworkNotSupported,
        NoAddressError,
        NoPrivateKeyFound,
        NotGranted,
        ParametersInvalid,
        TooManyOperations,
        TransactionInvalid,
        Aborted,
        Unknown,
        Internal,
    }
}