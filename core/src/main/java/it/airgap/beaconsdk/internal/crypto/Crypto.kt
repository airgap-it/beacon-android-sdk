package it.airgap.beaconsdk.internal.crypto

import it.airgap.beaconsdk.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.internal.crypto.data.SessionKeyPair
import it.airgap.beaconsdk.internal.crypto.provider.CryptoProvider
import it.airgap.beaconsdk.internal.utils.*
import it.airgap.beaconsdk.internal.utils.HexString
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.asHexString
import it.airgap.beaconsdk.internal.utils.flatTryInternal
import it.airgap.beaconsdk.internal.utils.isHex
import it.airgap.beaconsdk.internal.utils.tryInternal

internal class Crypto(private val cryptoProvider: CryptoProvider) {
    fun hashKey(key: HexString): InternalResult<ByteArray> = hash(key, 32)
    fun hashKey(key: ByteArray): InternalResult<ByteArray> = hash(key, 32)

    fun hash(message: String, size: Int): InternalResult<ByteArray> =
        if (message.isHex()) hash(HexString.fromString(message), size)
        else hash(message.toByteArray(), size)

    fun hash(message: HexString, size: Int): InternalResult<ByteArray> =
        hash(message.asByteArray(), size)

    fun hash(message: ByteArray, size: Int): InternalResult<ByteArray> =
        tryInternal { cryptoProvider.getHash(message, size) }

    fun hashSha256(message: HexString): InternalResult<ByteArray> =
        hashSha256(message.asByteArray())

    fun hashSha256(message: ByteArray): InternalResult<ByteArray> =
        tryInternal { cryptoProvider.getHash256(message) }

    fun generateRandomSeed(): InternalResult<String> =
        tryInternal {
            val bytes = cryptoProvider.generateRandomBytes(SEED_BYTES)

            listOf(
                bytes.slice(0 until 4),
                bytes.slice(4 until 6),
                bytes.slice(6 until 8),
                bytes.slice(8 until 10),
                bytes.slice(10 until SEED_BYTES)
            ).joinToString("-") { slice ->
                slice.joinToString("") {
                    it.asHexString().value(withPrefix = false)
                }
            }
        }

    fun getKeyPairFromSeed(seed: String): InternalResult<KeyPair> =
        tryInternal {
            val seedHash = cryptoProvider.getHash(seed.toByteArray(), 32)

            cryptoProvider.getEd25519KeyPairFromSeed(seedHash)
        }

    fun createServerSessionKeyPair(publicKey: ByteArray, privateKey: ByteArray): InternalResult<SessionKeyPair> =
        tryInternal {
            val serverPublicKey = cryptoProvider.convertEd25519PublicKeyToCurve25519(privateKey.sliceArray(32 until 64))
            val serverPrivateKey = cryptoProvider.convertEd25519PrivateKeyToCurve25519(privateKey)
            val clientPublicKey = cryptoProvider.convertEd25519PublicKeyToCurve25519(publicKey)

            cryptoProvider.createServerSessionKeyPair(serverPublicKey, serverPrivateKey, clientPublicKey)
        }

    fun createClientSessionKeyPair(publicKey: ByteArray, privateKey: ByteArray): InternalResult<SessionKeyPair> =
        tryInternal {
            val serverPublicKey = cryptoProvider.convertEd25519PublicKeyToCurve25519(privateKey.sliceArray(32 until 64))
            val serverPrivateKey = cryptoProvider.convertEd25519PrivateKeyToCurve25519(privateKey)
            val clientPublicKey = cryptoProvider.convertEd25519PublicKeyToCurve25519(publicKey)

            cryptoProvider.createClientSessionKeyPair(serverPublicKey, serverPrivateKey, clientPublicKey)
        }

    fun validateEncryptedMessage(encrypted: String): Boolean = cryptoProvider.validateMessage(encrypted)

    fun encryptMessageWithPublicKey(message: String, publicKey: ByteArray): InternalResult<String> =
        flatTryInternal {
            val hexMessage =
                if (message.isHex()) HexString.fromString(message)
                else message.toByteArray().asHexString()

            encryptMessageWithPublicKey(hexMessage, publicKey)
        }

    fun encryptMessageWithPublicKey(message: HexString, publicKey: ByteArray): InternalResult<String> =
        tryInternal { cryptoProvider.encryptMessageWithPublicKey(message, publicKey) }

    fun decryptMessageWithKeyPair(message: String, publicKey: ByteArray, privateKey: ByteArray): InternalResult<String> =
        flatTryInternal {
            val hexMessage =
                if (message.isHex()) HexString.fromString(message)
                else message.toByteArray().asHexString()

            decryptMessageWithKeyPair(hexMessage, publicKey, privateKey)
        }

    fun decryptMessageWithKeyPair(message: HexString, publicKey: ByteArray, privateKey: ByteArray): InternalResult<String> =
        tryInternal { cryptoProvider.decryptMessageWithKeyPair(message, publicKey, privateKey) }

    fun encryptMessageWithSharedKey(message: String, sharedKey: ByteArray): InternalResult<String> =
        flatTryInternal {
            val hexMessage =
                if (message.isHex()) HexString.fromString(message)
                else message.toByteArray().asHexString()

            encryptMessageWithSharedKey(hexMessage, sharedKey)
        }

    fun encryptMessageWithSharedKey(message: HexString, sharedKey: ByteArray): InternalResult<String> =
        tryInternal { cryptoProvider.encryptMessageWithSharedKey(message, sharedKey) }

    fun decryptMessageWithSharedKey(message: String, sharedKey: ByteArray): InternalResult<String> =
        flatTryInternal {
            val hexMessage =
                if (message.isHex()) HexString.fromString(message)
                else message.toByteArray().asHexString()

            decryptMessageWithSharedKey(hexMessage, sharedKey)
        }

    fun decryptMessageWithSharedKey(message: HexString, sharedKey: ByteArray): InternalResult<String> =
        tryInternal { cryptoProvider.decryptMessageWithSharedKey(message, sharedKey) }

    companion object {
        private const val SEED_BYTES = 16
    }
}