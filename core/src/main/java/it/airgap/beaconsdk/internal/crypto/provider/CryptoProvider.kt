package it.airgap.beaconsdk.internal.crypto.provider

import it.airgap.beaconsdk.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.internal.crypto.data.SessionKeyPair
import it.airgap.beaconsdk.internal.utils.HexString

internal interface CryptoProvider {
    @Throws(Exception::class)
    fun generateRandomBytes(length: Int): ByteArray

    @Throws(Exception::class)
    fun getHash(message: ByteArray, size: Int): ByteArray
    @Throws(Exception::class)
    fun getHash256(message: ByteArray): ByteArray

    @Throws(Exception::class)
    fun getEd25519KeyPairFromSeed(seed: ByteArray): KeyPair

    @Throws(Exception::class)
    fun convertEd25519PrivateKeyToCurve25519(privateKey: ByteArray): ByteArray
    @Throws(Exception::class)
    fun convertEd25519PublicKeyToCurve25519(publicKey: ByteArray): ByteArray

    @Throws(Exception::class)
    fun createServerSessionKeyPair(serverPublicKey: ByteArray, serverPrivateKey: ByteArray, clientPublicKey: ByteArray): SessionKeyPair
    @Throws(Exception::class)
    fun createClientSessionKeyPair(serverPublicKey: ByteArray, serverPrivateKey: ByteArray, clientPublicKey: ByteArray): SessionKeyPair

    fun validateMessage(message: String): Boolean

    @Throws(Exception::class)
    fun encryptMessage(message: HexString, sharedKey: ByteArray): String
    @Throws(Exception::class)
    fun decryptMessage(message: HexString, sharedKey: ByteArray): String
}