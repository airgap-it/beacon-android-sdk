package it.airgap.beaconsdk.data.beacon

import it.airgap.beaconsdk.message.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Types of errors supported in Beacon.
 */
@Serializable
public enum class BeaconError {
    /**
     * Indicates that the transaction broadcast failed.
     *
     * Applicable to [OperationBeaconRequest] and [BroadcastBeaconRequest].
     */
    @SerialName("BROADCAST_ERROR")
    BroadcastError,

    /**
     * Indicates that the specified network is not supported by the wallet.
     *
     * Applicable to [PermissionBeaconRequest].
     */
    @SerialName("NETWORK_NOT_SUPPORTED_ERROR")
    NetworkNotSupported,

    /**
     * Indicates that there is no address present for the protocol or specified network.
     *
     * Applicable to [PermissionBeaconRequest].
     */
    @SerialName("NO_ADDRESS_ERROR")
    NoAddressError,

    /**
     * Indicates that a private key matching the address provided in the request could not be found.
     *
     * Applicable to [SignPayloadBeaconRequest].
     */
    @SerialName("NO_PRIVATE_KEY_FOUND_ERROR")
    NoPrivateKeyFound,

    /**
     * Indicates that the signature was blocked and could not be completed ([SignPayloadBeaconRequest])
     * or the permissions requested by the dApp were rejected ([PermissionBeaconRequest]).
     *
     * Applicable to [SignPayloadBeaconRequest] and [PermissionBeaconRequest].
     */
    @SerialName("NOT_GRANTED_ERROR")
    NotGranted,

    /**
     * Indicates that any of the provided parameters are invalid.
     *
     * Applicable to [OperationBeaconRequest].
     */
    @SerialName("PARAMETERS_INVALID_ERROR")
    ParametersInvalid,

    /**
     * Indicates that too many operation details were included in the request
     * and they could not be included into a single operation group.
     *
     * Applicable to [OperationBeaconRequest].
     */
    @SerialName("TOO_MANY_OPERATIONS_ERROR")
    TooManyOperations,

    /**
     * Indicates that the transaction included in the request could not be parsed or was rejected by the node.
     *
     * Applicable to [BroadcastBeaconRequest].
     */
    @SerialName("TRANSACTION_INVALID_ERROR")
    TransactionInvalid,

    /**
     * Indicates that the requested type of signature is not supported in the client.
     *
     * Applicable to [SignPayloadBeaconRequest].
     */
    @SerialName("SIGNATURE_TYPE_NOT_SUPPORTED")
    SignatureTypeNotSupported,

    /**
     * Indicates that the request execution has been aborted by the user or the wallet.
     *
     * Applicable to every [BeaconRequest].
     */
    @SerialName("ABORTED_ERROR")
    Aborted,

    /**
     * Indicates that an unexpected error occurred.
     *
     * Applicable to every [BeaconRequest].
     */
    @SerialName("UNKNOWN_ERROR")
    Unknown,
}