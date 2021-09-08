package it.airgap.beaconsdk.core.internal.utils

import it.airgap.beaconsdk.core.data.beacon.Network
import it.airgap.beaconsdk.core.internal.crypto.Crypto

internal class AccountUtils(private val crypto: Crypto, private val base58Check: Base58Check) {

    fun getAccountIdentifier(address: String, network: Network): Result<String> {
        val data = with(network) {
            mutableListOf(address, identifier).apply {
                name?.let { add("name:$it") }
                rpcUrl?.let { add("rpc:$it") }
            }.toList()
        }

        val hash = crypto.hash(data.joinToString("-"), 10)

        return hash.flatMap { base58Check.encode(it) }
    }

    fun getSenderId(publicKey: ByteArray): Result<String> {
        val hash = crypto.hash(publicKey, SENDER_ID_HASH_SIZE)
        return hash.flatMap { base58Check.encode(it) }
    }

    companion object {
        private const val SENDER_ID_HASH_SIZE = 5
    }
}