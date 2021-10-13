package it.airgap.beaconsdk.blockchain.tezos.message.request

import it.airgap.beaconsdk.blockchain.tezos.data.TezosNetwork
import it.airgap.beaconsdk.blockchain.tezos.data.operation.TezosOperation
import it.airgap.beaconsdk.blockchain.tezos.message.response.BroadcastTezosResponse
import it.airgap.beaconsdk.blockchain.tezos.message.response.OperationTezosResponse
import it.airgap.beaconsdk.blockchain.tezos.message.response.SignPayloadTezosResponse
import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.data.SigningType
import it.airgap.beaconsdk.core.message.BlockchainBeaconRequest

/**
 * Base class for requests specific to Tezos used in the Beacon communication.
 */
public sealed class BlockchainTezosRequest : BlockchainBeaconRequest() {
    public companion object {}
}

/**
 * Message requesting the broadcast of the given [Tezos operations][operationDetails].
 * The operations may be only partially filled by the dApp and lack certain information.
 *
 * Expects [OperationTezosResponse] as a response.
 *
 * @property [id] The value that identifies this request.
 * @property [version] The message version.
 * @property [blockchainIdentifier] The unique name of the blockchain that specifies this request.
 * @property [senderId] The value that identifies the sender of this request.
 * @property [appMetadata] The metadata describing the dApp asking for the broadcast. May be `null` if the [senderId] is unknown.
 * @property [origin] The origination data of this request.
 * @property [network] The network on which the operations should be broadcast.
 * @property [operationDetails] Tezos operations which should be broadcast.
 * @property [sourceAddress] The address of the Tezos account that is requested to broadcast the operations.
 */
public data class OperationTezosRequest internal constructor(
    override val id: String,
    override val version: String,
    override val blockchainIdentifier: String,
    override val senderId: String,
    override val appMetadata: AppMetadata?,
    override val origin: Origin,
    public val network: TezosNetwork,
    public val operationDetails: List<TezosOperation>,
    public val sourceAddress: String,
) : BlockchainTezosRequest() {
    public companion object {}
}

/**
 * Message requesting the signature of the given [payload].
 *
 * Expects [SignPayloadTezosResponse] as a response.
 *
 * @property [id] The value that identifies this request.
 * @property [version] The message version.
 * @property [blockchainIdentifier] The unique name of the blockchain that specifies this request.
 * @property [senderId] The value that identifies the sender of this request.
 * @property [appMetadata] The metadata describing the dApp asking for the signature. May be `null` if the [senderId] is unknown.
 * @property [signingType] The requested type of signature. The client MUST fail if cannot provide the specified signature.
 * @property [payload] The payload to be signed.
 * @property [sourceAddress] The address of the account with which the payload should be signed.
 * @property [origin] The origination data of this request.
 */
public data class SignPayloadTezosRequest internal constructor(
    override val id: String,
    override val version: String,
    override val blockchainIdentifier: String,
    override val senderId: String,
    override val appMetadata: AppMetadata?,
    override val origin: Origin,
    public val signingType: SigningType,
    public val payload: String,
    public val sourceAddress: String,
) : BlockchainTezosRequest() {
    public companion object {}
}

/**
 * Message requesting the broadcast of the given [transaction][signedTransaction].
 *
 * Expects [BroadcastTezosResponse] as a response.
 *
 * @property [id] The value that identifies this request.
 * @property [version] The message version.
 * @property [blockchainIdentifier] The unique name of the blockchain that specifies this request.
 * @property [senderId] The value that identifies the sender of this request.
 * @property [appMetadata] The metadata describing the dApp asking for the broadcast. May be `null` if the [senderId] is unknown.
 * @property [network] The network on which the transaction should be broadcast.
 * @property [signedTransaction] The transaction to be broadcast.
 * @property [origin] The origination data of this request.
 */
public data class BroadcastTezosRequest internal constructor(
    override val id: String,
    override val version: String,
    override val blockchainIdentifier: String,
    override val senderId: String,
    override val appMetadata: AppMetadata?,
    override val origin: Origin,
    public val network: TezosNetwork,
    public val signedTransaction: String,
) : BlockchainTezosRequest() {
    public companion object {}
}