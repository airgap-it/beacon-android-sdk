package it.airgap.beaconsdk.core.internal.crypto.provider

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.core.internal.crypto.data.SessionKeyPair

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CryptoProvider {
    @Throws(Exception::class)
    public fun generateRandomBytes(length: Int): ByteArray

    @Throws(Exception::class)
    public fun getHash(message: ByteArray, size: Int): ByteArray

    @Throws(Exception::class)
    public fun getHash256(message: ByteArray): ByteArray

    @Throws(Exception::class)
    public fun getEd25519KeyPairFromSeed(seed: ByteArray): KeyPair

    @Throws(Exception::class)
    public fun convertEd25519PrivateKeyToCurve25519(privateKey: ByteArray): ByteArray

    @Throws(Exception::class)
    public fun convertEd25519PublicKeyToCurve25519(publicKey: ByteArray): ByteArray

    @Throws(Exception::class)
    public fun createServerSessionKeyPair(
        serverPublicKey: ByteArray,
        serverPrivateKey: ByteArray,
        clientPublicKey: ByteArray,
    ): SessionKeyPair

    @Throws(Exception::class)
    public fun createClientSessionKeyPair(
        serverPublicKey: ByteArray,
        serverPrivateKey: ByteArray,
        clientPublicKey: ByteArray,
    ): SessionKeyPair

    @Throws(Exception::class)
    public fun signMessageDetached(message: ByteArray, privateKey: ByteArray): ByteArray

    public fun validateMessage(message: String): Boolean

    @Throws(Exception::class)
    public fun encryptMessageWithPublicKey(message: ByteArray, publicKey: ByteArray): ByteArray

    @Throws(Exception::class)
    public fun decryptMessageWithKeyPair(
        message: ByteArray,
        publicKey: ByteArray,
        privateKey: ByteArray,
    ): ByteArray

    @Throws(Exception::class)
    public fun encryptMessageWithSharedKey(message: ByteArray, sharedKey: ByteArray): ByteArray

    @Throws(Exception::class)
    public fun decryptMessageWithSharedKey(message: ByteArray, sharedKey: ByteArray): ByteArray
}