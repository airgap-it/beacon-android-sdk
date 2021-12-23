package it.airgap.beaconsdk.blockchain.substrate.data

import it.airgap.beaconsdk.core.data.Network
import it.airgap.beaconsdk.blockchain.substrate.Substrate
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Substrate network data.
 *
 * @property [blockchainIdentifier] A unique name of the blockchain which the network applies to.
 * @property [genesisHash] The genesis hash of the chain.
 * @property [name] An optional name of the network.
 * @property [rpcUrl] An optional URL for the network RPC interface.
 */
@Serializable
public data class SubstrateNetwork(
    val genesisHash: String,
    override val name: String? = null,
    override val rpcUrl: String? = null,
) : Network() {
    @Transient
    override val blockchainIdentifier: String = Substrate.IDENTIFIER

    override val identifier: String
        get() = mutableListOf(genesisHash).apply {
            name?.let { add("name:$it") }
            rpcUrl?.let { add("rpc:$it") }
        }.joinToString("-")

    public companion object {}
}