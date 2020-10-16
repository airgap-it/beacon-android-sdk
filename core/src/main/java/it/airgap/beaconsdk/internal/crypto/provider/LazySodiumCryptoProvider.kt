package it.airgap.beaconsdk.internal.crypto.provider

import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.interfaces.*
import com.goterl.lazycode.lazysodium.utils.Key
import it.airgap.beaconsdk.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.internal.crypto.data.SessionKeyPair
import it.airgap.beaconsdk.internal.utils.HexString
import it.airgap.beaconsdk.internal.utils.asHexString

internal class LazySodiumCryptoProvider : CryptoProvider {
    private val sodium: LazySodiumAndroid = LazySodiumAndroid(SodiumAndroid())

    @Throws(Exception::class)
    override fun generateRandomBytes(length: Int): ByteArray =
        (sodium as Random).randomBytesBuf(length)

    @Throws(Exception::class)
    override fun getHash(message: ByteArray, size: Int): ByteArray {
        val genericHashSodium = sodium as GenericHash.Native

        return ByteArray(size).also { genericHashSodium.cryptoGenericHash(it, it.size, message, message.size.toLong()) }
    }

    override fun getHash256(message: ByteArray): ByteArray {
        val hashSodium = sodium as Hash.Native

        return ByteArray(message.size).also { hashSodium.cryptoHashSha256(it, message, message.size.toLong()) }
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

        val curve = ByteArray(Sign.CURVE25519_SECRETKEYBYTES)
        signSodium.convertSecretKeyEd25519ToCurve25519(curve, privateKey)

        return curve
    }

    @Throws(Exception::class)
    override fun convertEd25519PublicKeyToCurve25519(publicKey: ByteArray): ByteArray {
        val signSodium = sodium as Sign.Native

        val curve = ByteArray(Sign.CURVE25519_PUBLICKEYBYTES)
        signSodium.convertPublicKeyEd25519ToCurve25519(curve, publicKey)

        return curve
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

    override fun validateMessage(message: String): Boolean =
        try {
            HexString.fromString(message).length(withPrefix = false) >= SecretBox.NONCEBYTES + SecretBox.MACBYTES
        } catch (e: Exception) {
            false
        }

    @Throws(Exception::class)
    override fun encryptMessage(message: HexString, sharedKey: ByteArray): String {
        val randomSodium = sodium as Random
        val secretBoxSodium = sodium as SecretBox.Lazy

        val nonce = randomSodium.randomBytesBuf(SecretBox.NONCEBYTES)
        val key = Key.fromBytes(sharedKey)

        val encrypted = secretBoxSodium.cryptoSecretBoxEasy(message.value(withPrefix = false), nonce, key)

        return "${nonce.asHexString().value(withPrefix = false)}${encrypted}"
    }

    @Throws(Exception::class)
    override fun decryptMessage(message: HexString, sharedKey: ByteArray): String {
        val secretBoxSodium = sodium as SecretBox.Lazy

        val nonce = message.slice(0 until SecretBox.NONCEBYTES)
        val ciphertext = message.slice(SecretBox.NONCEBYTES)
        val key = Key.fromBytes(sharedKey)

        return secretBoxSodium.cryptoSecretBoxOpenEasy(ciphertext.value(withPrefix = false), nonce.asByteArray(), key)
    }
}