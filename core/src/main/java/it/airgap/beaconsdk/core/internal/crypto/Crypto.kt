package it.airgap.beaconsdk.core.internal.crypto

import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.core.internal.crypto.data.SessionKeyPair
import it.airgap.beaconsdk.core.internal.crypto.provider.CryptoProvider
import it.airgap.beaconsdk.core.internal.data.HexString
import it.airgap.beaconsdk.core.internal.utils.runCatchingFlat
import it.airgap.beaconsdk.core.internal.utils.toHexString

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Crypto(private val cryptoProvider: CryptoProvider) {
    public fun hashKey(key: HexString): Result<ByteArray> = hashKey(key.toByteArray())
    public fun hashKey(key: ByteArray): Result<ByteArray> = hash(key, key.size)

    public fun hash(message: String, @IntRange(from = 1) size: Int): Result<ByteArray> =
        hash(message.toByteArray(), size)

    public fun hash(message: HexString, @IntRange(from = 1) size: Int): Result<ByteArray> =
        hash(message.toByteArray(), size)

    public fun hash(message: ByteArray, @IntRange(from = 1) size: Int): Result<ByteArray> =
        runCatching { cryptoProvider.getHash(message, size) }

    public fun hashSha256(message: HexString): Result<ByteArray> =
        hashSha256(message.toByteArray())

    public fun hashSha256(message: ByteArray): Result<ByteArray> =
        runCatching { cryptoProvider.getHash256(message) }

    public fun guid(): Result<String> =
        runCatching {
            val bytes = cryptoProvider.generateRandomBytes(SEED_BYTES)

            listOf(
                bytes.slice(0 until 4),
                bytes.slice(4 until 6),
                bytes.slice(6 until 8),
                bytes.slice(8 until 10),
                bytes.slice(10 until SEED_BYTES)
            ).joinToString("-") { slice ->
                slice.joinToString("") {
                    it.toHexString().asString()
                }
            }
        }

    public fun getKeyPairFromSeed(seed: String): Result<KeyPair> =
        runCatching {
            val seedHash = cryptoProvider.getHash(seed.encodeToByteArray(), 32)

            cryptoProvider.getEd25519KeyPairFromSeed(seedHash)
        }

    public fun createServerSessionKeyPair(
        publicKey: ByteArray,
        privateKey: ByteArray,
    ): Result<SessionKeyPair> =
        runCatching {
            val serverPublicKey = cryptoProvider.convertEd25519PublicKeyToCurve25519(privateKey.sliceArray(32 until 64))
            val serverPrivateKey = cryptoProvider.convertEd25519PrivateKeyToCurve25519(privateKey)
            val clientPublicKey = cryptoProvider.convertEd25519PublicKeyToCurve25519(publicKey)

            cryptoProvider.createServerSessionKeyPair(
                serverPublicKey,
                serverPrivateKey,
                clientPublicKey,
            )
        }

    public fun createClientSessionKeyPair(
        publicKey: ByteArray,
        privateKey: ByteArray,
    ): Result<SessionKeyPair> =
        runCatching {
            val serverPublicKey = cryptoProvider.convertEd25519PublicKeyToCurve25519(privateKey.sliceArray(32 until 64))
            val serverPrivateKey = cryptoProvider.convertEd25519PrivateKeyToCurve25519(privateKey)
            val clientPublicKey = cryptoProvider.convertEd25519PublicKeyToCurve25519(publicKey)

            cryptoProvider.createClientSessionKeyPair(
                serverPublicKey,
                serverPrivateKey,
                clientPublicKey,
            )
        }

    public fun signMessageDetached(message: String, privateKey: ByteArray): Result<ByteArray> =
        runCatchingFlat { signMessageDetached(message.encodeToByteArray(), privateKey) }

    public fun signMessageDetached(message: HexString, privateKey: ByteArray): Result<ByteArray> =
        runCatchingFlat { signMessageDetached(message.toByteArray(), privateKey) }

    public fun signMessageDetached(message: ByteArray, privateKey: ByteArray): Result<ByteArray> =
        runCatching { cryptoProvider.signMessageDetached(message, privateKey) }

    public fun validateEncryptedMessage(encrypted: String): Boolean = cryptoProvider.validateMessage(encrypted)

    public fun encryptMessageWithPublicKey(
        message: String,
        publicKey: ByteArray,
    ): Result<ByteArray> =
        runCatchingFlat { encryptMessageWithPublicKey(message.encodeToByteArray(), publicKey) }

    public fun encryptMessageWithPublicKey(
        message: HexString,
        publicKey: ByteArray,
    ): Result<ByteArray> =
        runCatchingFlat { encryptMessageWithPublicKey(message.toByteArray(), publicKey) }

    public fun encryptMessageWithPublicKey(
        message: ByteArray,
        publicKey: ByteArray,
    ): Result<ByteArray> =
        runCatching {
            val curve25519Key = cryptoProvider.convertEd25519PublicKeyToCurve25519(publicKey)
            cryptoProvider.encryptMessageWithPublicKey(message, curve25519Key)
        }

    public fun decryptMessageWithKeyPair(
        message: String,
        publicKey: ByteArray,
        privateKey: ByteArray,
    ): Result<ByteArray> =
        runCatchingFlat { decryptMessageWithKeyPair(message.encodeToByteArray(), publicKey, privateKey) }

    public fun decryptMessageWithKeyPair(
        message: HexString,
        publicKey: ByteArray,
        privateKey: ByteArray,
    ): Result<ByteArray> =
        runCatchingFlat { decryptMessageWithKeyPair(message.toByteArray(), publicKey, privateKey) }

    public fun decryptMessageWithKeyPair(
        message: ByteArray,
        publicKey: ByteArray,
        privateKey: ByteArray,
    ): Result<ByteArray> =
        runCatching {
            val curve25519PublicKey = cryptoProvider.convertEd25519PublicKeyToCurve25519(publicKey)
            val curve25519PrivateKey = cryptoProvider.convertEd25519PrivateKeyToCurve25519(privateKey)

            cryptoProvider.decryptMessageWithKeyPair(message, curve25519PublicKey, curve25519PrivateKey)
        }

    public fun encryptMessageWithSharedKey(
        message: String,
        sharedKey: ByteArray,
    ): Result<ByteArray> =
        runCatchingFlat { encryptMessageWithSharedKey(message.encodeToByteArray(), sharedKey) }

    public fun encryptMessageWithSharedKey(
        message: HexString,
        sharedKey: ByteArray,
    ): Result<ByteArray> =
        runCatchingFlat { encryptMessageWithSharedKey(message.toByteArray(), sharedKey) }

    public fun encryptMessageWithSharedKey(
        message: ByteArray,
        sharedKey: ByteArray,
    ): Result<ByteArray> =
        runCatching { cryptoProvider.encryptMessageWithSharedKey(message, sharedKey) }

    public fun decryptMessageWithSharedKey(
        message: String,
        sharedKey: ByteArray,
    ): Result<ByteArray> =
        runCatchingFlat { decryptMessageWithSharedKey(message.encodeToByteArray(), sharedKey) }

    public fun decryptMessageWithSharedKey(
        message: HexString,
        sharedKey: ByteArray,
    ): Result<ByteArray> =
        runCatchingFlat { decryptMessageWithSharedKey(message.toByteArray(), sharedKey) }

    public fun decryptMessageWithSharedKey(
        message: ByteArray,
        sharedKey: ByteArray,
    ): Result<ByteArray> =
        runCatching { cryptoProvider.decryptMessageWithSharedKey(message, sharedKey) }

    public companion object {
        private const val SEED_BYTES = 16
    }
}