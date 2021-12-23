package it.airgap.beaconsdk.blockchain.substrate.data

import kotlinx.serialization.Serializable

@Serializable
public data class SubstrateRuntimeSpec(
    public val runtimeVersion: String,
    public val transactionVersion: String,
) {
    public companion object {}
}
