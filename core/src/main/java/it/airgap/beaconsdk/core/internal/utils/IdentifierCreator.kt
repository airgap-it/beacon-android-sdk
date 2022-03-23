package it.airgap.beaconsdk.core.internal.utils

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.Network
import it.airgap.beaconsdk.core.internal.crypto.Crypto

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class IdentifierCreator internal constructor(
    private val crypto: Crypto,
    private val base58Check: Base58Check,
) {
    public fun accountId(address: String, network: Network?): Result<String> {
        val input = network?.let { "$address-${it.identifier}" } ?: address
        val hash = crypto.hash(input, 10)

        return hash.flatMap { base58Check.encode(it) }
    }

    public fun senderId(publicKey: ByteArray): Result<String> {
        val hash = crypto.hash(publicKey, SENDER_ID_HASH_SIZE)
        return hash.flatMap { base58Check.encode(it) }
    }

    public companion object {
        private const val SENDER_ID_HASH_SIZE = 5
    }
}