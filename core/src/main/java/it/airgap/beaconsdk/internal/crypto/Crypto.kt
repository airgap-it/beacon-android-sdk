package it.airgap.beaconsdk.internal.crypto

import androidx.annotation.IntRange
import it.airgap.beaconsdk.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.internal.crypto.data.SessionKeyPair
import it.airgap.beaconsdk.internal.crypto.provider.CryptoProvider
import it.airgap.beaconsdk.internal.utils.*

internal class Crypto(private val cryptoProvider: CryptoProvider) {
    fun hashKey(key: HexString): InternalResult<ByteArray> = hashKey(key.asByteArray())
    fun hashKey(key: ByteArray): InternalResult<ByteArray> = hash(key, key.size)

    fun hash(message: String, @IntRange(from = 1) size: Int): InternalResult<ByteArray> =
        hash(message.toByteArray(), size)

    fun hash(message: HexString, @IntRange(from = 1) size: Int): InternalResult<ByteArray> =
        hash(message.asByteArray(), size)

    fun hash(message: ByteArray, @IntRange(from = 1) size: Int): InternalResult<ByteArray> =
        tryResult { cryptoProvider.getHash(message, size) }

    fun hashSha256(message: HexString): InternalResult<ByteArray> =
        hashSha256(message.asByteArray())

    fun hashSha256(message: ByteArray): InternalResult<ByteArray> =
        tryResult { cryptoProvider.getHash256(message) }

    fun guid(): InternalResult<String> =
        tryResult {
            val bytes = cryptoProvider.generateRandomBytes(SEED_BYTES)

            listOf(
                bytes.slice(0 until 4),
                bytes.slice(4 until 6),
                bytes.slice(6 until 8),
                bytes.slice(8 until 10),
                bytes.slice(10 until SEED_BYTES)
            ).joinToString("-") { slice ->
                slice.joinToString("") {
                    it.asHexString().asString()
                }
            }
        }

    fun getKeyPairFromSeed(seed: String): InternalResult<KeyPair> =
        tryResult {
            val seedHash = cryptoProvider.getHash(seed.encodeToByteArray(), 32)

            cryptoProvider.getEd25519KeyPairFromSeed(seedHash)
        }

    fun createServerSessionKeyPair(
        publicKey: ByteArray,
        privateKey: ByteArray,
    ): InternalResult<SessionKeyPair> =
        tryResult {
            val serverPublicKey = cryptoProvider.convertEd25519PublicKeyToCurve25519(privateKey.sliceArray(32 until 64))
            val serverPrivateKey = cryptoProvider.convertEd25519PrivateKeyToCurve25519(privateKey)
            val clientPublicKey = cryptoProvider.convertEd25519PublicKeyToCurve25519(publicKey)

            cryptoProvider.createServerSessionKeyPair(
                serverPublicKey,
                serverPrivateKey,
                clientPublicKey,
            )
        }

    fun createClientSessionKeyPair(
        publicKey: ByteArray,
        privateKey: ByteArray,
    ): InternalResult<SessionKeyPair> =
        tryResult {
            val serverPublicKey = cryptoProvider.convertEd25519PublicKeyToCurve25519(privateKey.sliceArray(32 until 64))
            val serverPrivateKey = cryptoProvider.convertEd25519PrivateKeyToCurve25519(privateKey)
            val clientPublicKey = cryptoProvider.convertEd25519PublicKeyToCurve25519(publicKey)

            cryptoProvider.createClientSessionKeyPair(
                serverPublicKey,
                serverPrivateKey,
                clientPublicKey,
            )
        }

    fun signMessageDetached(message: String, privateKey: ByteArray): InternalResult<ByteArray> =
        flatTryResult { signMessageDetached(message.encodeToByteArray(), privateKey) }

    fun signMessageDetached(message: HexString, privateKey: ByteArray): InternalResult<ByteArray> =
        flatTryResult { signMessageDetached(message.asByteArray(), privateKey) }

    fun signMessageDetached(message: ByteArray, privateKey: ByteArray): InternalResult<ByteArray> =
        tryResult { cryptoProvider.signMessageDetached(message, privateKey) }

    fun validateEncryptedMessage(encrypted: String): Boolean = cryptoProvider.validateMessage(encrypted)

    fun encryptMessageWithPublicKey(
        message: String,
        publicKey: ByteArray,
    ): InternalResult<ByteArray> =
        flatTryResult { encryptMessageWithPublicKey(message.encodeToByteArray(), publicKey) }

    fun encryptMessageWithPublicKey(
        message: HexString,
        publicKey: ByteArray,
    ): InternalResult<ByteArray> =
        flatTryResult { encryptMessageWithPublicKey(message.asByteArray(), publicKey) }

    fun encryptMessageWithPublicKey(
        message: ByteArray,
        publicKey: ByteArray,
    ): InternalResult<ByteArray> =
        tryResult {
            val curve25519Key = cryptoProvider.convertEd25519PublicKeyToCurve25519(publicKey)
            cryptoProvider.encryptMessageWithPublicKey(message, curve25519Key)
        }

    fun decryptMessageWithKeyPair(
        message: String,
        publicKey: ByteArray,
        privateKey: ByteArray,
    ): InternalResult<ByteArray> =
        flatTryResult { decryptMessageWithKeyPair(message.encodeToByteArray(), publicKey, privateKey) }

    fun decryptMessageWithKeyPair(
        message: HexString,
        publicKey: ByteArray,
        privateKey: ByteArray,
    ): InternalResult<ByteArray> =
        flatTryResult { decryptMessageWithKeyPair(message.asByteArray(), publicKey, privateKey) }

    fun decryptMessageWithKeyPair(
        message: ByteArray,
        publicKey: ByteArray,
        privateKey: ByteArray,
    ): InternalResult<ByteArray> =
        tryResult {
            val curve25519PublicKey = cryptoProvider.convertEd25519PublicKeyToCurve25519(publicKey)
            val curve25519PrivateKey = cryptoProvider.convertEd25519PrivateKeyToCurve25519(privateKey)

            cryptoProvider.decryptMessageWithKeyPair(message, curve25519PublicKey, curve25519PrivateKey)
        }

    fun encryptMessageWithSharedKey(
        message: String,
        sharedKey: ByteArray,
    ): InternalResult<ByteArray> =
        flatTryResult { encryptMessageWithSharedKey(message.encodeToByteArray(), sharedKey) }

    fun encryptMessageWithSharedKey(
        message: HexString,
        sharedKey: ByteArray,
    ): InternalResult<ByteArray> =
        flatTryResult { encryptMessageWithSharedKey(message.asByteArray(), sharedKey) }

    fun encryptMessageWithSharedKey(
        message: ByteArray,
        sharedKey: ByteArray,
    ): InternalResult<ByteArray> =
        tryResult { cryptoProvider.encryptMessageWithSharedKey(message, sharedKey) }

    fun decryptMessageWithSharedKey(
        message: String,
        sharedKey: ByteArray,
    ): InternalResult<ByteArray> =
        flatTryResult { decryptMessageWithSharedKey(message.encodeToByteArray(), sharedKey) }

    fun decryptMessageWithSharedKey(
        message: HexString,
        sharedKey: ByteArray,
    ): InternalResult<ByteArray> =
        flatTryResult { decryptMessageWithSharedKey(message.asByteArray(), sharedKey) }

    fun decryptMessageWithSharedKey(
        message: ByteArray,
        sharedKey: ByteArray,
    ): InternalResult<ByteArray> =
        tryResult { cryptoProvider.decryptMessageWithSharedKey(message, sharedKey) }

    companion object {
        private const val SEED_BYTES = 16
    }
}