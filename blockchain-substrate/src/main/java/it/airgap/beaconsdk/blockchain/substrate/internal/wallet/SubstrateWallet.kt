package it.airgap.beaconsdk.blockchain.substrate.internal.wallet

import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.utils.*

internal class SubstrateWallet(
    private val crypto: Crypto,
    private val base58: Base58,
) {
    fun address(publicKey: String, prefix: Int): Result<String> = runCatchingFlat {
        val version = prefix.toByteArray(trim = true)
        val payload = publicKey.asHexString().toByteArray().also { if (it.size != 32) failWithInvalidPublicKey() }

        val checksum = checksum(version + payload).getOrThrow()
        val checksumBytes = if (payload.size == 32) 2 else 1

        base58.encode(version + payload + checksum.sliceArray(0 until checksumBytes))
    }

    private fun checksum(input: ByteArray): Result<ByteArray> =
        crypto.hash(CONTEXT_PREFIX.encodeToByteArray() + input, 64)

    private fun failWithInvalidPublicKey(): Nothing = failWithIllegalArgument("Public key is invalid, expected a 32-byte hex string")

    companion object {
        const val CONTEXT_PREFIX = "SS58PRE"
    }
}