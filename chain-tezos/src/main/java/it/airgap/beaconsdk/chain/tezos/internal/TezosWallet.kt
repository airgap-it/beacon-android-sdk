package it.airgap.beaconsdk.chain.tezos.internal

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.chain.tezos.Tezos
import it.airgap.beaconsdk.core.internal.chain.Chain
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.utils.*

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TezosWallet internal constructor(private val crypto: Crypto, private val base58Check: Base58Check) : Chain.Wallet {
    override fun addressFromPublicKey(publicKey: String): Result<String> =
        runCatchingFlat {
            val publicKeyBytes = with(publicKey) {
                when {
                    isPlainPublicKey() -> asHexString().toByteArray()
                    isEncryptedPublicKey() -> publicKeyFromEncrypted(this)
                    else -> failWithInvalidPublicKey()
                }
            }

            val payload = crypto.hash(publicKeyBytes, 20)
            payload.flatMap { base58Check.encode(Tezos.PrefixBytes.tz1 + it) }
        }

    private fun publicKeyFromEncrypted(encrypted: String): ByteArray {
        val decoded = base58Check.decode(encrypted).getOrThrow()

        return decoded.sliceArray(Tezos.Prefix.edpk.length until decoded.size)
    }

    private fun String.isPlainPublicKey(): Boolean = ((length == 64 || length == 66) && isHex())
    private fun String.isEncryptedPublicKey(): Boolean = (length == 54 && startsWith(Tezos.Prefix.edpk))

    private fun failWithInvalidPublicKey(): Nothing =
        throw IllegalArgumentException("Public key is invalid, expected a plain hex string or encrypted key")
}