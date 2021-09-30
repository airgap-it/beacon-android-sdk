package it.airgap.beaconsdk.blockchain.tezos.data.operation

import kotlinx.serialization.Serializable

@Serializable
public data class TezosInlinedEndorsement(
    public val branch: String,
    public val operations: TezosEndorsementOperation,
    public val signature: String? = null,
) {
    public companion object {}
}