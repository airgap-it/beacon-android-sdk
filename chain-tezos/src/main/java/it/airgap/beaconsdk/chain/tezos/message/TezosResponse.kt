package it.airgap.beaconsdk.chain.tezos.message

import it.airgap.beaconsdk.core.data.beacon.SigningType
import it.airgap.beaconsdk.core.message.ChainBeaconResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Base class for responses specific to Tezos used in the Beacon communication.
 */
@Serializable
public sealed class TezosResponse: ChainBeaconResponse.Payload() {
    public companion object {}
}

/**
 * Message payload responding to [OperationTezosRequest].
 *
 * @property [transactionHash] The hash of the broadcast operations.
 */
@Serializable
@SerialName("operation_response")
public data class OperationTezosResponse(
    public val transactionHash: String,
) : TezosResponse() {
    public companion object {}
}

/**
 * Message payload responding to [SignPayloadTezosRequest].
 *
 * @property [signingType] The signature type.
 * @property [signature] The payload signature.
 */
@Serializable
@SerialName("sign_payload_response")
public data class SignPayloadTezosResponse(
    public val signingType: SigningType,
    public val signature: String,
) : TezosResponse() {
    public companion object {}
}


/**
 * Message payload responding to [BroadcastTezosRequest].
 *
 * @property [transactionHash] The hash of the broadcast transaction.
 */
@Serializable
@SerialName("broadcast_response")
public data class BroadcastTezosResponse(
    public val transactionHash: String,
) : TezosResponse() {
    public companion object {}
}
