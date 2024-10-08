package it.airgap.beaconsdk.core.exception

import it.airgap.beaconsdk.core.data.Connection

/**
 * Base for exceptions recognized in the Beacon SDK.
 *
 * @property message An optional detailed error message.
 * @property cause An optional cause of the error.
 */
public sealed class BeaconException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {

    public companion object {
        public fun from(exception: Throwable?): BeaconException =
            when {
                exception is BeaconException -> exception
                exception != null -> InternalException(cause = exception)
                else -> UnknownException()
            }
    }
}

/**
 * Thrown if multiple connections have failed to execute an action.
 *
 * @property errors A list of detailed errors that caused the exception.
 * @property types A list of connection types that failed.
 */
public class MultipleConnectionException(
    public val errors: List<ConnectionException>,
) : BeaconException("Multiple connections failed") {
    public val types: List<Connection.Type> = errors.map { it.type }

    public companion object {}
}

/**
 * Thrown when an action could not be executed on the specified connection.
 *
 * @property type The connection type for which the action failed.
 */
public class ConnectionException(
    public val type: Connection.Type,
    message: String? = null,
    cause: Throwable? = null,
) : BeaconException(message, cause) {

    public companion object {
        internal fun from(type: Connection.Type, exception: Throwable): ConnectionException =
            when (exception) {
                is ConnectionException -> if (type == exception.type) exception else ConnectionException(type, cause = exception)
                else -> ConnectionException(type, cause = exception)
            }
    }
}

/**
 * Throw when an action required a specific blockchain to be registered, but it could not be found.
 *
 * @property blockchainIdentifier The identifier of missing blockchain.
 */
public class BlockchainNotFoundException(public val blockchainIdentifier: String) : BeaconException("Blockchain \"$blockchainIdentifier\" could not be found")

/**
 * Thrown if an internal error occurred.
 */
public class InternalException(message: String? = null, cause: Throwable? = null) : BeaconException(message ?: "Internal error", cause) {
    public companion object {}
}

/**
 * Thrown if an unknown error occurred.
 */
public class UnknownException : BeaconException("Unknown error") {
    public companion object {}
}