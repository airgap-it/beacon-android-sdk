package it.airgap.beaconsdk.blockchain.tezos.internal.wallet

import it.airgap.beaconsdk.blockchain.tezos.Tezos
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.utils.*

internal class TezosWallet internal constructor(private val crypto: Crypto, private val base58Check: Base58Check) {
    fun address(publicKey: String): Result<String> =
        runCatchingFlat {
            val publicKey = PublicKey.fromString(publicKey) ?: failWithInvalidPublicKey()
            val payload = crypto.hash(publicKey.bytes, 20)

            payload.flatMap { base58Check.encode(publicKey.prefix.toAddress() + it) }
        }

    private fun PublicKey.Companion.fromString(string: String): PublicKey? = with(string) {
        when {
            isPlainPublicKey() -> PublicKey.fromPlain(this)
            isEncryptedPublicKey() -> PublicKey.fromEncrypted(this)
            else -> null
        }
    }

    private fun PublicKey.Companion.fromPlain(plain: String): PublicKey {
        val bytes = plain.asHexString().toByteArray()
        return PublicKey(Tezos.Prefix.PublicKey.Ed25519, bytes)
    }

    private fun PublicKey.Companion.fromEncrypted(encrypted: String): PublicKey {
        val prefix = encrypted.publicKeyPrefix() ?: failWithInvalidPublicKey()
        val decoded = base58Check.decode(encrypted).getOrThrow()
        val bytes = decoded.sliceArray(prefix.bytes.size until decoded.size)

        return PublicKey(prefix, bytes)
    }

    private fun String.isPlainPublicKey(): Boolean = isHex() && (length == 64 || length == 66)
    private fun String.isEncryptedPublicKey(): Boolean {
        val prefix = publicKeyPrefix() ?: return false
        return length == prefix.encodedLength
    }

    private fun String.publicKeyPrefix(): Tezos.Prefix.PublicKey? = Tezos.Prefix.PublicKey.values().find { startsWith(it) }
    private fun String.startsWith(prefix: Tezos.Prefix): Boolean = startsWith(prefix.value)

    private operator fun Tezos.Prefix.plus(bytes: ByteArray): ByteArray = this.bytes + bytes
    private fun Tezos.Prefix.PublicKey.toAddress(): Tezos.Prefix.Address =
        when(this) {
            Tezos.Prefix.PublicKey.Ed25519 -> Tezos.Prefix.Address.Ed25519
            Tezos.Prefix.PublicKey.Secp256K1 -> Tezos.Prefix.Address.Secp256K1
            Tezos.Prefix.PublicKey.P256 -> Tezos.Prefix.Address.P256
        }

    private fun failWithInvalidPublicKey(): Nothing = failWithIllegalArgument("Public key is invalid, expected a plain hex string or encrypted key")

    private class PublicKey(val prefix: Tezos.Prefix.PublicKey, val bytes: ByteArray) {
        companion object {}
    }
}