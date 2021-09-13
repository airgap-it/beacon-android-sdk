package it.airgap.beaconsdk.core.internal.utils

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.beacon.Network
import it.airgap.beaconsdk.core.internal.crypto.Crypto

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AccountUtils internal constructor(private val crypto: Crypto, private val base58Check: Base58Check) {

    public fun getAccountIdentifier(address: String, network: Network): Result<String> {
        val data = with(network) {
            mutableListOf(address, identifier).apply {
                name?.let { add("name:$it") }
                rpcUrl?.let { add("rpc:$it") }
            }.toList()
        }

        val hash = crypto.hash(data.joinToString("-"), 10)

        return hash.flatMap { base58Check.encode(it) }
    }

    public fun getSenderId(publicKey: ByteArray): Result<String> {
        val hash = crypto.hash(publicKey, SENDER_ID_HASH_SIZE)
        return hash.flatMap { base58Check.encode(it) }
    }

    public companion object {
        private const val SENDER_ID_HASH_SIZE = 5
    }
}