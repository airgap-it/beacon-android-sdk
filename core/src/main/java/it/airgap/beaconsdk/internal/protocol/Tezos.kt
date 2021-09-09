package it.airgap.beaconsdk.internal.protocol

import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.utils.*

internal class Tezos(private val crypto: Crypto, private val base58Check: Base58Check) : Protocol {
    private val tz1Prefix: ByteArray = byteArrayOf(6, 161.toByte(), 159.toByte())

    override fun getAddressFromPublicKey(publicKey: String): Result<String> =
        runCatchingFlat {
            val publicKeyBytes = with(publicKey) {
                when {
                    isPlainPublicKey() -> asHexString().toByteArray()
                    isEncryptedPublicKey() -> publicKeyFromEncrypted(this)
                    else -> failWithInvalidPublicKey()
                }
            }

            val payload = crypto.hash(publicKeyBytes, 20)
            payload.flatMap { base58Check.encode(tz1Prefix + it) }
        }

    private fun publicKeyFromEncrypted(encrypted: String): ByteArray {
        val decoded = base58Check.decode(encrypted).getOrThrow()

        return decoded.sliceArray(PREFIX_ENCRYPTED_PUBLIC_KEY.length until decoded.size)
    }

    private fun String.isPlainPublicKey(): Boolean = ((length == 64 || length == 66) && isHex())
    private fun String.isEncryptedPublicKey(): Boolean =
        (length == 54 && startsWith(PREFIX_ENCRYPTED_PUBLIC_KEY))

    private fun failWithInvalidPublicKey(): Nothing =
        throw IllegalArgumentException("Public key is invalid, expected a plain hex string or encrypted key")

    companion object {
        private const val PREFIX_ENCRYPTED_PUBLIC_KEY = "edpk"
    }
}