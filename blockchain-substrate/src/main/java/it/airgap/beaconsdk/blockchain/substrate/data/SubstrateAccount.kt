package it.airgap.beaconsdk.blockchain.substrate.data

import kotlinx.serialization.Serializable

/**
 * Substrate account data.
 *
 * @property [network] The network on which the account is valid.
 * @property [addressPrefix] An address type prefix that identifies an address as belonging to a specific network.
 * @property [publicKey] The public key that identifies the account.
 */
@Serializable
public data class SubstrateAccount(
    public val network: SubstrateNetwork,
    public val addressPrefix: Int,
    public val publicKey: String,
) {
    public companion object {}
}
