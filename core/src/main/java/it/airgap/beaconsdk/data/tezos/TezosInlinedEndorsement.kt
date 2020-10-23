package it.airgap.beaconsdk.data.tezos

import kotlinx.serialization.Serializable

@Serializable
data class TezosInlinedEndorsement(
    val branch: String,
    val operations: TezosOperation.Endorsement,
    val signature: String?
)