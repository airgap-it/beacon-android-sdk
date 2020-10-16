package it.airgap.beaconsdk.internal.utils

import it.airgap.beaconsdk.data.network.Network
import it.airgap.beaconsdk.internal.crypto.Crypto

internal class AccountUtils(private val crypto: Crypto, private val base58Check: Base58Check) {

    fun getAccountIdentifier(address: String, network: Network): InternalResult<String> {
        val data = mutableListOf(address, network.type.identifier)
        network.name?.let { data.add("name:$it") }
        network.rpcUrl?.let { data.add("rpc:$it") }

        val hash = crypto.hash(data.joinToString("-"), 10)

        return hash.flatMap { base58Check.encode(it) }
    }
}