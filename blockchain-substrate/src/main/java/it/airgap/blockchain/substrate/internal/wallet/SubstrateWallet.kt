package it.airgap.blockchain.substrate.internal.wallet

import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.utils.Base58
import it.airgap.beaconsdk.core.internal.utils.asHexString
import it.airgap.beaconsdk.core.internal.utils.runCatchingFlat
import it.airgap.beaconsdk.core.internal.utils.toByteArray

internal class SubstrateWallet(
    private val base58: Base58,
    private val crypto: Crypto,
) {
    fun address(publicKey: String, prefix: Int): Result<String> = runCatchingFlat {
        val version = prefix.toByteArray(trim = true)
        val payload = publicKey.asHexString().toByteArray()
        val checksum = checksum(version + payload).getOrThrow()
        val checksumBytes = if (payload.size == 32) 2 else 1

        base58.encode(version + payload + checksum.sliceArray(0 until checksumBytes))
    }

    private fun checksum(input: ByteArray): Result<ByteArray> =
        crypto.hash(CONTEXT_PREFIX.encodeToByteArray() + input, 512)

    companion object {
        const val CONTEXT_PREFIX = "SS58PRE"
    }
}