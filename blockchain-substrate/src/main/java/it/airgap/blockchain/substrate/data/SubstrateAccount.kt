package it.airgap.blockchain.substrate.data

import kotlinx.serialization.Serializable

@Serializable
public data class SubstrateAccount(
    public val network: SubstrateNetwork,
    public val addressPrefix: Int,
    public val publicKey: String,
) {
    public companion object {}
}
