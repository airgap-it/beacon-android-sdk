package it.airgap.beaconsdk.blockchain.tezos.message.response

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.blockchain.tezos.message.request.BroadcastTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.OperationTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.SignPayloadTezosRequest
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.data.SigningType
import it.airgap.beaconsdk.core.message.BlockchainBeaconResponse

/**
 * Base class for responses specific to Tezos used in the Beacon communication.
 */
public sealed class BlockchainTezosResponse : BlockchainBeaconResponse() {
    public companion object {}
}

/**
 * Message payload responding to [OperationTezosRequest].
 *
 * @property [id] The value that identifies the request to which the message is responding.
 * @property [version] The message version.
 * @property [destination] The origination data of the request.
 * @property [blockchainIdentifier] A unique name of the blockchain that specifies the request.
 * @property [transactionHash] The hash of the broadcast operations.
 */
public data class OperationTezosResponse internal constructor(
    override val id: String,
    override val version: String,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override val destination: Connection.Id,
    override val blockchainIdentifier: String,
    public val transactionHash: String,
) : BlockchainTezosResponse() {
    public companion object {

        /**
         * Creates a new instance of [OperationTezosResponse] from the [request]
         * with the specified [transactionHash].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(request: OperationTezosRequest, transactionHash: String): OperationTezosResponse =
            OperationTezosResponse(request.id, request.version, request.origin, request.blockchainIdentifier, transactionHash)

    }
}

/**
 * Message payload responding to [SignPayloadTezosRequest].
 *
 * @property [id] The value that identifies the request to which the message is responding.
 * @property [version] The message version.
 * @property [destination] The origination data of the request.
 * @property [blockchainIdentifier] A unique name of the blockchain that specifies the request.
 * @property [signingType] The signature type.
 * @property [signature] The payload signature.
 */
public data class SignPayloadTezosResponse internal constructor(
    override val id: String,
    override val version: String,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override val destination: Connection.Id,
    override val blockchainIdentifier: String,
    public val signingType: SigningType,
    public val signature: String,
) : BlockchainTezosResponse() {
    public companion object {

        /**
         * Creates a new instance of [SignPayloadTezosResponse] from the [request]
         * with the specified [signingType] and [signature].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(request: SignPayloadTezosRequest, signingType: SigningType = request.signingType, signature: String): SignPayloadTezosResponse =
            SignPayloadTezosResponse(request.id, request.version, request.origin, request.blockchainIdentifier, signingType, signature)

    }
}


/**
 * Message payload responding to [BroadcastTezosRequest].
 *
 * @property [id] The value that identifies the request to which the message is responding.
 * @property [version] The message version.
 * @property [destination] The origination data of the request.
 * @property [blockchainIdentifier] A unique name of the blockchain that specifies the request.
 * @property [transactionHash] The hash of the broadcast transaction.
 */
public data class BroadcastTezosResponse internal constructor(
    override val id: String,
    override val version: String,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override val destination: Connection.Id,
    override val blockchainIdentifier: String,
    public val transactionHash: String,
) : BlockchainTezosResponse() {
    public companion object {

        /**
         * Creates a new instance of [BroadcastTezosResponse] from the [request]
         * with the specified [transactionHash].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(request: BroadcastTezosRequest, transactionHash: String): BroadcastTezosResponse =
            BroadcastTezosResponse(request.id, request.version, request.origin, request.blockchainIdentifier, transactionHash)

    }
}
