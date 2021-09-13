package it.airgap.beaconsdk.chain.tezos.message

import it.airgap.beaconsdk.core.data.beacon.Network
import it.airgap.beaconsdk.core.data.beacon.SigningType
import it.airgap.beaconsdk.core.data.tezos.TezosOperation
import it.airgap.beaconsdk.core.message.ChainBeaconRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Base class for requests specific to Tezos used in the Beacon communication.
 */
@Serializable
public sealed class TezosRequest: ChainBeaconRequest.Payload() {
    public companion object {}
}

/**
 * Message payload requesting the broadcast of the given [Tezos operations][operationDetails].
 * The operations may be only partially filled by the dApp and lack certain information.
 *
 * Expects [OperationTezosResponse] as a response payload.
 *
 * @property [network] The network on which the operations should be broadcast.
 * @property [operationDetails] Tezos operations which should be broadcast.
 * @property [sourceAddress] The address of the Tezos account that is requested to broadcast the operations.
 */
@Serializable
@SerialName("operation_request")
public data class OperationTezosRequest internal constructor(
    public val network: Network,
    public val operationDetails: List<TezosOperation>,
    public val sourceAddress: String,
) : TezosRequest() {
    public companion object {}
}

/**
 * Message payload requesting the signature of the given [payload].
 *
 * Expects [SignPayloadTezosResponse] as a response payload.
 *
 * @property [signingType] The requested type of signature. The client MUST fail if cannot provide the specified signature.
 * @property [payload] The payload to be signed.
 * @property [sourceAddress] The address of the account with which the payload should be signed.
 */
@Serializable
@SerialName("sign_payload_request")
public data class SignPayloadTezosRequest internal constructor(
    public val signingType: SigningType,
    public val payload: String,
    public val sourceAddress: String,
) : TezosRequest() {
    public companion object {}
}

/**
 * Message payload requesting the broadcast of the given [transaction][signedTransaction].
 *
 * Expects [BroadcastTezosResponse] as a response payload.
 *
 * @property [network] The network on which the transaction should be broadcast.
 * @property [signedTransaction] The transaction to be broadcast.
 */
@Serializable
@SerialName("broadcast_request")
public data class BroadcastTezosRequest internal constructor(
    public val network: Network,
    public val signedTransaction: String,
) : TezosRequest() {
    public companion object {}
}