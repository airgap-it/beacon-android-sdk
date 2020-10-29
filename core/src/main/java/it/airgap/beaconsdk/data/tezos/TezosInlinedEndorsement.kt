package it.airgap.beaconsdk.data.tezos

import kotlinx.serialization.Serializable

@Serializable
public data class TezosInlinedEndorsement(
    public val branch: String,
    public val operations: TezosOperation.Endorsement,
    public val signature: String?,
)