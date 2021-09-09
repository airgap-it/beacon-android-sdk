package it.airgap.beaconsdk.core.internal.crypto.provider

import it.airgap.beaconsdk.core.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.core.internal.crypto.data.SessionKeyPair
import it.airgap.beaconsdk.core.internal.utils.failWith

public class MockCryptoProvider(
    var shouldFail: Boolean = false,
    var messageValid: Boolean = true,
) : CryptoProvider {

    @Throws(Exception::class)
    override fun generateRandomBytes(length: Int): ByteArray =
        if (shouldFail) failWith()
        else (0 until length).map { it.toByte() }.toByteArray()

    @Throws(Exception::class)
    override fun getHash(message: ByteArray, size: Int): ByteArray =
        if (shouldFail) failWith()
        else message.copyOf(size)

    @Throws(Exception::class)
    override fun getHash256(message: ByteArray): ByteArray =
        if (shouldFail) failWith()
        else message.copyOf(32)

    @Throws(Exception::class)
    override fun getEd25519KeyPairFromSeed(seed: ByteArray): KeyPair =
        if (shouldFail) failWith()
        else KeyPair(
            ByteArray(64) { 0 },
            ByteArray(32) { 0 },
        )

    @Throws(Exception::class)
    override fun convertEd25519PrivateKeyToCurve25519(privateKey: ByteArray): ByteArray =
        if (shouldFail) failWith()
        else privateKey

    @Throws(Exception::class)
    override fun convertEd25519PublicKeyToCurve25519(publicKey: ByteArray): ByteArray =
        if (shouldFail) failWith()
        else publicKey

    @Throws(Exception::class)
    override fun createServerSessionKeyPair(
        serverPublicKey: ByteArray,
        serverPrivateKey: ByteArray,
        clientPublicKey: ByteArray,
    ): SessionKeyPair =
        if (shouldFail) failWith()
        else SessionKeyPair(
            ByteArray(32) { 0 },
            ByteArray(32) { 0 },
        )

    @Throws(Exception::class)
    override fun createClientSessionKeyPair(
        serverPublicKey: ByteArray,
        serverPrivateKey: ByteArray,
        clientPublicKey: ByteArray,
    ): SessionKeyPair =
        if (shouldFail) failWith()
        else SessionKeyPair(
            ByteArray(32) { 0 },
            ByteArray(32) { 0 },
        )

    @Throws(Exception::class)
    override fun signMessageDetached(message: ByteArray, privateKey: ByteArray): ByteArray =
        if (shouldFail) failWith()
        else message

    @Throws(Exception::class)
    override fun validateMessage(message: String): Boolean =
        messageValid && (message.startsWith("!valid:") && message.substringAfter(":").toBoolean() )

    @Throws(Exception::class)
    override fun encryptMessageWithPublicKey(message: ByteArray, publicKey: ByteArray): ByteArray =
        if (shouldFail) failWith()
        else message

    @Throws(Exception::class)
    override fun decryptMessageWithKeyPair(
        message: ByteArray,
        publicKey: ByteArray,
        privateKey: ByteArray,
    ): ByteArray =
        if (shouldFail) failWith()
        else message

    @Throws(Exception::class)
    override fun encryptMessageWithSharedKey(message: ByteArray, sharedKey: ByteArray): ByteArray =
        if (shouldFail) failWith()
        else message

    @Throws(Exception::class)
    override fun decryptMessageWithSharedKey(message: ByteArray, sharedKey: ByteArray): ByteArray =
        if (shouldFail) failWith()
        else message
}