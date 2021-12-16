package it.airgap.blockchain.substrate.data

import it.airgap.beaconsdk.core.data.Network
import it.airgap.blockchain.substrate.Substrate
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
public data class SubstrateNetwork(
    val genesisHash: String,
    override val name: String?,
    override val rpcUrl: String?,
) : Network() {
    @Transient
    override val blockchainIdentifier: String = Substrate.IDENTIFIER

    override val identifier: String
        get() = mutableListOf(genesisHash).apply {
            name?.let { add("name:$it") }
            rpcUrl?.let { add("rpc:$it") }
        }.joinToString("-")
}