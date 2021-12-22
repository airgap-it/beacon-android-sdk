package it.airgap.beaconsdk.core.internal.utils

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.crypto.Crypto

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Base58Check internal constructor(private val base58: Base58, private val crypto: Crypto) {

    public fun encode(bytes: ByteArray): Result<String> =
        runCatchingFlat {
            val checksum = createChecksum(bytes)
            val encoded = base58.encode(bytes + checksum)

            encoded
        }

    public fun decode(base58check: String): Result<ByteArray> =
        runCatching {
            val (payload, checksum) = base58.decode(base58check).getOrThrow().splitAt { it.size - 4 }
            val newChecksum = createChecksum(payload)
            if (!checksum.contentEquals(newChecksum)) failWithInvalidChecksum()

            payload
        }

    private fun createChecksum(bytes: ByteArray): ByteArray {
        val hash = crypto.hashSha256(crypto.hashSha256(bytes).getOrThrow()).getOrThrow()

        return hash.sliceArray(0 until 4)
    }

    private fun failWithInvalidChecksum(): Nothing = throw IllegalArgumentException("Base58Check checksum is invalid")
}