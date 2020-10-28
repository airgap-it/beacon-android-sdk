package it.airgap.beaconsdk.internal.crypto.provider

import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.exceptions.SodiumException
import com.goterl.lazycode.lazysodium.interfaces.*
import com.goterl.lazycode.lazysodium.utils.Key
import it.airgap.beaconsdk.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.internal.crypto.data.SessionKeyPair
import it.airgap.beaconsdk.internal.utils.HexString
import it.airgap.beaconsdk.internal.utils.asHexString

private typealias SodiumKeyPair = com.goterl.lazycode.lazysodium.utils.KeyPair

internal class LazySodiumCryptoProvider : CryptoProvider {
    private val sodium: LazySodiumAndroid = LazySodiumAndroid(SodiumAndroid())

    @Throws(Exception::class)
    override fun generateRandomBytes(length: Int): ByteArray =
        (sodium as Random).randomBytesBuf(length)

    @Throws(Exception::class)
    override fun getHash(message: ByteArray, size: Int): ByteArray {
        val genericHashSodium = sodium as GenericHash.Native

        return ByteArray(size).also {
            assertOrFail("Could not hash the message") {
                genericHashSodium.cryptoGenericHash(it, it.size, message, message.size.toLong())
            }
        }
    }

    override fun getHash256(message: ByteArray): ByteArray {
        val hashSodium = sodium as Hash.Native

        return ByteArray(Hash.SHA256_BYTES).also {
            assertOrFail("Could not finalize SHA-256") {
                hashSodium.cryptoHashSha256(it, message, message.size.toLong())
            }
        }
    }

    @Throws(Exception::class)
    override fun getEd25519KeyPairFromSeed(seed: ByteArray): KeyPair {
        val signSodium = sodium as Sign.Lazy
        val keyPair = signSodium.cryptoSignSeedKeypair(seed)

        return KeyPair(keyPair.secretKey.asBytes, keyPair.publicKey.asBytes)
    }

    @Throws(Exception::class)
    override fun convertEd25519PrivateKeyToCurve25519(privateKey: ByteArray): ByteArray {
        val signSodium = sodium as Sign.Native

        return ByteArray(Sign.CURVE25519_SECRETKEYBYTES).also {
            assertOrFail("Could not convert the private key") {
                signSodium.convertSecretKeyEd25519ToCurve25519(it, privateKey)
            }
        }
    }

    @Throws(Exception::class)
    override fun convertEd25519PublicKeyToCurve25519(publicKey: ByteArray): ByteArray {
        val signSodium = sodium as Sign.Native

        return ByteArray(Sign.CURVE25519_PUBLICKEYBYTES).also {
            assertOrFail("Could not convert the public key") {
                signSodium.convertPublicKeyEd25519ToCurve25519(it, publicKey)
            }
        }
    }

    @Throws(Exception::class)
    override fun createServerSessionKeyPair(serverPublicKey: ByteArray, serverPrivateKey: ByteArray, clientPublicKey: ByteArray): SessionKeyPair {
        val kxSodium = sodium as KeyExchange.Lazy

        val serverPk = Key.fromBytes(serverPublicKey)
        val serverSk = Key.fromBytes(serverPrivateKey)
        val clientPk = Key.fromBytes(clientPublicKey)

        val sessionPair = kxSodium.cryptoKxServerSessionKeys(serverPk, serverSk, clientPk)

        return SessionKeyPair(sessionPair.rx, sessionPair.tx)
    }

    @Throws(Exception::class)
    override fun createClientSessionKeyPair(serverPublicKey: ByteArray, serverPrivateKey: ByteArray, clientPublicKey: ByteArray): SessionKeyPair {
        val kxSodium = sodium as KeyExchange.Lazy

        val serverPk = Key.fromBytes(serverPublicKey)
        val serverSk = Key.fromBytes(serverPrivateKey)
        val clientPk = Key.fromBytes(clientPublicKey)

        val sessionPair = kxSodium.cryptoKxClientSessionKeys(serverPk, serverSk, clientPk)

        return SessionKeyPair(sessionPair.rx, sessionPair.tx)
    }

    override fun signMessageDetached(message: ByteArray, privateKey: ByteArray): ByteArray {
        val signSodium = sodium as Sign.Native


        return ByteArray(Sign.ED25519_BYTES).also {
            assertOrFail("Could not create a signature for the message in the detached mode") {
                signSodium.cryptoSignDetached(
                    it,
                    message,
                    message.size.toLong(),
                    privateKey
                )
            }
        }
    }

    override fun validateMessage(message: String): Boolean =
        try {
            HexString.fromString(message).length(withPrefix = false) >= SecretBox.NONCEBYTES + SecretBox.MACBYTES
        } catch (e: Exception) {
            false
        }

    @Throws(Exception::class)
    override fun encryptMessageWithPublicKey(message: ByteArray, publicKey: ByteArray): ByteArray {
        val signSodium = sodium as Sign.Native
        val secretBoxSodium = sodium as Box.Native

        val kxPublicKey = ByteArray(Sign.CURVE25519_PUBLICKEYBYTES).also {
            assertOrFail("Could not convert the public key") {
                signSodium.convertPublicKeyEd25519ToCurve25519(it, publicKey)
            }
        }

        return ByteArray(Box.SEALBYTES + message.size).also {
            assertOrFail("Could not encrypt the message") {
                secretBoxSodium.cryptoBoxSeal(it, message, message.size.toLong(), kxPublicKey)
            }
        }
    }

    @Throws(Exception::class)
    override fun decryptMessageWithKeyPair(
        message: ByteArray,
        publicKey: ByteArray,
        privateKey: ByteArray
    ): ByteArray {
        val signSodium = sodium as Sign.Lazy
        val secretBoxSodium = sodium as Box.Native

        val edKeyPair = SodiumKeyPair(Key.fromBytes(privateKey), Key.fromBytes(publicKey))
        val kxKeyPair = signSodium.convertKeyPairEd25519ToCurve25519(edKeyPair)

        return ByteArray(message.size - Box.SEALBYTES).also {
            assertOrFail("Could not decrypt the message") {
                secretBoxSodium.cryptoBoxSealOpen(it, message, message.size.toLong(), kxKeyPair.publicKey.asBytes, kxKeyPair.secretKey.asBytes)
            }
        }.trimNulls()
    }

    @Throws(Exception::class)
    override fun encryptMessageWithSharedKey(message: ByteArray, sharedKey: ByteArray): ByteArray {
        val randomSodium = sodium as Random
        val secretBoxSodium = sodium as SecretBox.Native

        val nonce = randomSodium.randomBytesBuf(SecretBox.NONCEBYTES)

        val encrypted = ByteArray(SecretBox.MACBYTES + message.size).also {
            assertOrFail("Could not encrypt the message with the shared key") {
                secretBoxSodium.cryptoSecretBoxEasy(it, message, message.size.toLong(), nonce, sharedKey)
            }
        }

        return HexString.fromString("${nonce.asHexString().value()}${encrypted.asHexString().value()}").asByteArray()
    }

    @Throws(Exception::class)
    override fun decryptMessageWithSharedKey(message: ByteArray, sharedKey: ByteArray): ByteArray {
        val secretBoxSodium = sodium as SecretBox.Native

        val nonce = message.sliceArray(0 until SecretBox.NONCEBYTES)
        val ciphertext = message.sliceArray(SecretBox.NONCEBYTES until message.size)

        return ByteArray(message.size - SecretBox.MACBYTES).also {
            assertOrFail("Could not decrypt the message with the shared key") {
                secretBoxSodium.cryptoSecretBoxOpenEasy(it, ciphertext, ciphertext.size.toLong(), nonce, sharedKey)
            }
        }.trimNulls()
    }

    private fun ByteArray.trimNulls(): ByteArray = sodium.removeNulls(this)

    private inline fun assertOrFail(message: String? = null, block: () -> Boolean) {
        if (!block()) throw SodiumException(message)
    }
}