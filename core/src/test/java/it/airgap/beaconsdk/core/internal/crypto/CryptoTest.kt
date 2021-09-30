package it.airgap.beaconsdk.core.internal.crypto

import io.mockk.confirmVerified
import io.mockk.spyk
import io.mockk.verify
import it.airgap.beaconsdk.core.internal.crypto.provider.MockCryptoProvider
import it.airgap.beaconsdk.core.internal.utils.asHexString
import it.airgap.beaconsdk.core.internal.utils.splitAt
import it.airgap.beaconsdk.core.internal.utils.toHexString
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class CryptoTest {
    private lateinit var cryptoProvider: MockCryptoProvider
    private lateinit var crypto: Crypto

    @Before
    fun setup() {
        cryptoProvider = spyk(MockCryptoProvider())
        crypto = Crypto(cryptoProvider)
    }

    @Test
    fun `hashes key provided as HexString`() {
        val key = ByteArray(1) { 0 }
        val hash = crypto.hashKey(key.toHexString())

        assertTrue(hash.isSuccess, "Expected hash result to be a success")
        verify { cryptoProvider.getHash(key, key.size) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `hashes key provided as ByteArray`() {
        val key = ByteArray(1) { 0 }
        val hash = crypto.hashKey(key)

        assertTrue(hash.isSuccess, "Expected hash result to be a success")
        verify { cryptoProvider.getHash(key, key.size) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `returns failure result if failed to hash HexString key`() {
        cryptoProvider.shouldFail = true

        val key = ByteArray(1) { 0 }
        val hash = crypto.hashKey(key.toHexString())

        assertTrue(hash.isFailure, "Expected hash result to be a failure")
    }

    @Test
    fun `returns failure result if failed to hash ByteArray key`() {
        cryptoProvider.shouldFail = true

        val key = ByteArray(1) { 0 }
        val hash = crypto.hashKey(key)

        assertTrue(hash.isFailure, "Expected hash result to be a failure")
    }

    @Test
    fun `hashes string message with specified size`() {
        val messagesWithSize = listOf(
            "message#1" to 1,
            "message#2" to 16,
            "message#3" to 100,
        )

        messagesWithSize
            .forEach {
                val hash = crypto.hash(it.first, it.second)

                assertTrue(hash.isSuccess, "Expected hash result to be a success")
                assertEquals(it.second, hash.getOrThrow().size)

                verify { cryptoProvider.getHash(it.first.encodeToByteArray(), it.second) }
            }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `hashes HexString message with specified size`() {
        val messagesWithSize = listOf(
            "9434dc98" to 1,
            "9434dc98" to 16,
            "9434dc98" to 100,
        ).map { it.first.asHexString() to it.second }

        messagesWithSize
            .forEach {
                val hash = crypto.hash(it.first, it.second)

                assertTrue(hash.isSuccess, "Expected hash result to be a success")
                assertEquals(it.second, hash.getOrThrow().size)

                verify { cryptoProvider.getHash(it.first.toByteArray(), it.second) }
            }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `hashes ByteArray message with specified size`() {
        val messagesWithSize = listOf(
            "9434dc98" to 1,
            "9434dc98" to 16,
            "9434dc98" to 100,
        ).map { it.first.asHexString().toByteArray() to it.second }

        messagesWithSize
            .forEach {
                val hash = crypto.hash(it.first, it.second)

                assertTrue(hash.isSuccess, "Expected hash result to be a success")
                assertEquals(it.second, hash.getOrThrow().size)

                verify { cryptoProvider.getHash(it.first, it.second) }
            }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `returns failure result when string message hash failed`() {
        cryptoProvider.shouldFail = true

        val message = "message"
        val size = 32

        val hash = crypto.hash(message, size)

        assertTrue(hash.isFailure, "Expected hash result to be a failure")
    }

    @Test
    fun `returns failure result when HexString message hash failed`() {
        cryptoProvider.shouldFail = true

        val message = "9434dc98".asHexString()
        val size = 32

        val hash = crypto.hash(message, size)

        assertTrue(hash.isFailure, "Expected hash result to be a failure")
    }

    @Test
    fun `returns failure result when ByteArray message hash failed`() {
        cryptoProvider.shouldFail = true

        val message = "9434dc98".asHexString().toByteArray()
        val size = 32

        val hash = crypto.hash(message, size)

        assertTrue(hash.isFailure, "Expected hash result to be a failure")
    }

    @Test
    fun `creates SHA-256 hash from HexString message`() {
        val message = "9434dc98".asHexString()

        val hash = crypto.hashSha256(message)

        assertTrue(hash.isSuccess, "Expected hash result to be a success")
        verify { cryptoProvider.getHash256(message.toByteArray()) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `creates SHA-256 hash from ByteArray message`() {
        val message = "9434dc98".asHexString().toByteArray()

        val hash = crypto.hashSha256(message)

        assertTrue(hash.isSuccess, "Expected hash result to be a success")
        verify { cryptoProvider.getHash256(message) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `returns failure result if failed to generate SHA-256 hash from HexString message`() {
        cryptoProvider.shouldFail = true

        val message = "9434dc98".asHexString()
        val hash = crypto.hashSha256(message)

        assertTrue(hash.isFailure, "Expected hash result to be a failure")
    }

    @Test
    fun `returns failure result if failed to generate SHA-256 hash from ByteArray message`() {
        cryptoProvider.shouldFail = true

        val message = "9434dc98".asHexString().toByteArray()
        val hash = crypto.hashSha256(message)

        assertTrue(hash.isFailure, "Expected hash result to be a failure")
    }

    @Test
    fun `creates random seed as UUID`() {
        val seed = crypto.guid().getOrThrow()

        assertEquals("00010203-0405-0607-0809-0a0b0c0d0e0f", seed)
    }

    @Test
    fun `returns failure result when random seed generation failed`() {
        cryptoProvider.shouldFail = true

        val seed = crypto.guid()

        assertTrue(seed.isFailure, "Expected seed result to be a failure")
    }

    @Test
    fun `creates key pair from seed`() {
        val seed = "seed"
        val keyPair = crypto.getKeyPairFromSeed(seed)

        assertTrue(keyPair.isSuccess, "Expected key pair result to be a success")
        verify { cryptoProvider.getHash(seed.encodeToByteArray(), 32) }
        verify { cryptoProvider.getEd25519KeyPairFromSeed(seed.encodeToByteArray().copyOf(32)) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `returns failure result if failed to create key pair from seed`() {
        cryptoProvider.shouldFail = true

        val seed = "seed"
        val keyPair = crypto.getKeyPairFromSeed(seed)

        assertTrue(keyPair.isFailure, "Expected key pair result to be a failure")
    }

    @Test
    fun `creates server session key pair`() {
        val (publicKey, privateKey) = (0 until 96).map { it.toByte() }.splitAt(32)
        val keyPair = crypto.createServerSessionKeyPair(publicKey.toByteArray(), privateKey.toByteArray())

        val serverPublicKey = privateKey.slice(32 until 64).toByteArray()
        val serverPrivateKey = privateKey.toByteArray()
        val clientPublicKey = publicKey.toByteArray()

        assertTrue(keyPair.isSuccess, "Expected key pair result to be a success")
        verify { cryptoProvider.convertEd25519PublicKeyToCurve25519(serverPublicKey) }
        verify { cryptoProvider.convertEd25519PrivateKeyToCurve25519(serverPrivateKey) }
        verify { cryptoProvider.convertEd25519PublicKeyToCurve25519(clientPublicKey) }
        verify { cryptoProvider.createServerSessionKeyPair(serverPublicKey, serverPrivateKey, clientPublicKey) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `returns failure result if failed to create server session key pair`() {
        cryptoProvider.shouldFail = true

        val keyPair = crypto.createServerSessionKeyPair(ByteArray(0), ByteArray(0))

        assertTrue(keyPair.isFailure, "Expected session key pair result to be a failure")
    }

    @Test
    fun `creates client session key pair`() {
        val (publicKey, privateKey) = (0 until 96).map { it.toByte() }.splitAt(32)
        val keyPair = crypto.createClientSessionKeyPair(publicKey.toByteArray(), privateKey.toByteArray())

        val serverPublicKey = privateKey.slice(32 until 64).toByteArray()
        val serverPrivateKey = privateKey.toByteArray()
        val clientPublicKey = publicKey.toByteArray()

        assertTrue(keyPair.isSuccess, "Expected key pair result to be a success")
        verify { cryptoProvider.convertEd25519PublicKeyToCurve25519(serverPublicKey) }
        verify { cryptoProvider.convertEd25519PrivateKeyToCurve25519(serverPrivateKey) }
        verify { cryptoProvider.convertEd25519PublicKeyToCurve25519(clientPublicKey) }
        verify { cryptoProvider.createClientSessionKeyPair(serverPublicKey, serverPrivateKey, clientPublicKey) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `returns failure result if failed to create client session key pair`() {
        cryptoProvider.shouldFail = true

        val keyPair = crypto.createClientSessionKeyPair(ByteArray(0), ByteArray(0))

        assertTrue(keyPair.isFailure, "Expected client key pair result to be a failure")
    }

    @Test
    fun `create signature for string message`() {
        val message = "message"
        val privateKey = ByteArray(0)

        val signature = crypto.signMessageDetached(message, privateKey)

        assertTrue(signature.isSuccess, "Expected signature result to be a success")
        verify { cryptoProvider.signMessageDetached(message.encodeToByteArray(), privateKey) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `create signature for HexString message`() {
        val message = "9434dc98".asHexString()
        val privateKey = ByteArray(0)

        val signature = crypto.signMessageDetached(message, privateKey)

        assertTrue(signature.isSuccess, "Expected signature result to be a success")
        verify { cryptoProvider.signMessageDetached(message.toByteArray(), privateKey) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `create signature for ByteArray message`() {
        val message = "9434dc98".asHexString().toByteArray()
        val privateKey = ByteArray(0)

        val signature = crypto.signMessageDetached(message, privateKey)

        assertTrue(signature.isSuccess, "Expected signature result to be a success")
        verify { cryptoProvider.signMessageDetached(message, privateKey) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `returns failure result if failed to create signature for string message`() {
        cryptoProvider.shouldFail = true

        val message = "message"
        val privateKey = ByteArray(0)

        val signature = crypto.signMessageDetached(message, privateKey)

        assertTrue(signature.isFailure, "Expected signature result to be a failure")
    }

    @Test
    fun `returns failure result if failed to create signature for HexString message`() {
        cryptoProvider.shouldFail = true

        val message = "9434dc98".asHexString()
        val privateKey = ByteArray(0)

        val signature = crypto.signMessageDetached(message, privateKey)

        assertTrue(signature.isFailure, "Expected signature result to be a failure")
    }

    @Test
    fun `returns failure result if failed to create signature for ByteArray message`() {
        cryptoProvider.shouldFail = true

        val message = "9434dc98".asHexString().toByteArray()
        val privateKey = ByteArray(0)

        val signature = crypto.signMessageDetached(message, privateKey)

        assertTrue(signature.isFailure, "Expected signature result to be a failure")
    }

    @Test
    fun `validates encrypted message`() {
        val valid = crypto.validateEncryptedMessage("!valid:true")
        val invalid = crypto.validateEncryptedMessage("!valid:false")

        assertTrue(valid, "Expected message to be recognized as valid")
        assertFalse(invalid, "Expected message to be recognized as invalid")
    }

    @Test
    fun `encrypts string message with public key`() {
        val message = "message"
        val publicKey = ByteArray(0)

        val encrypted = crypto.encryptMessageWithPublicKey(message, publicKey)

        assertTrue(encrypted.isSuccess, "Expected encryption result to be a success")
        verify { cryptoProvider.convertEd25519PublicKeyToCurve25519(publicKey) }
        verify { cryptoProvider.encryptMessageWithPublicKey(message.encodeToByteArray(), publicKey) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `encrypts HexString message with public key`() {
        val message = "9434dc98".asHexString()
        val publicKey = ByteArray(0)

        val encrypted = crypto.encryptMessageWithPublicKey(message, publicKey)

        assertTrue(encrypted.isSuccess, "Expected encryption result to be a success")
        verify { cryptoProvider.convertEd25519PublicKeyToCurve25519(publicKey) }
        verify { cryptoProvider.encryptMessageWithPublicKey(message.toByteArray(), publicKey) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `encrypts ByteArray message with public key`() {
        val message = "9434dc98".asHexString().toByteArray()
        val publicKey = ByteArray(0)

        val encrypted = crypto.encryptMessageWithPublicKey(message, publicKey)

        assertTrue(encrypted.isSuccess, "Expected encryption result to be a success")
        verify { cryptoProvider.convertEd25519PublicKeyToCurve25519(publicKey) }
        verify { cryptoProvider.encryptMessageWithPublicKey(message, publicKey) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `returns failure result if failed to encrypt string message with public key`() {
        cryptoProvider.shouldFail = true

        val message = "message"
        val publicKey = ByteArray(0)

        val encrypted = crypto.encryptMessageWithPublicKey(message, publicKey)

        assertTrue(encrypted.isFailure, "Expected encryption result to be a failure")
    }

    @Test
    fun `returns failure result if failed to encrypt HexString message with public key`() {
        cryptoProvider.shouldFail = true

        val message = "9434dc98".asHexString()
        val publicKey = ByteArray(0)

        val encrypted = crypto.encryptMessageWithPublicKey(message, publicKey)

        assertTrue(encrypted.isFailure, "Expected encryption result to be a failure")
    }

    @Test
    fun `returns failure result if failed to encrypt ByteArray message with public key`() {
        cryptoProvider.shouldFail = true

        val message = "9434dc98".asHexString().toByteArray()
        val publicKey = ByteArray(0)

        val encrypted = crypto.encryptMessageWithPublicKey(message, publicKey)

        assertTrue(encrypted.isFailure, "Expected encryption result to be a failure")
    }

    @Test
    fun `decrypts string message with public key`() {
        val message = "message"
        val publicKey = ByteArray(0)
        val privateKey = ByteArray(0)

        val decrypted = crypto.decryptMessageWithKeyPair(message, publicKey, privateKey)

        assertTrue(decrypted.isSuccess, "Expected decryption result to be a success")
        verify { cryptoProvider.convertEd25519PublicKeyToCurve25519(publicKey) }
        verify { cryptoProvider.convertEd25519PrivateKeyToCurve25519(privateKey) }
        verify { cryptoProvider.decryptMessageWithKeyPair(message.encodeToByteArray(), publicKey, privateKey) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `decrypts HexString message with public key`() {
        val message = "9434dc98".asHexString()
        val publicKey = ByteArray(0)
        val privateKey = ByteArray(0)

        val decrypted = crypto.decryptMessageWithKeyPair(message, publicKey, privateKey)

        assertTrue(decrypted.isSuccess, "Expected decryption result to be a success")
        verify { cryptoProvider.convertEd25519PublicKeyToCurve25519(publicKey) }
        verify { cryptoProvider.convertEd25519PrivateKeyToCurve25519(privateKey) }
        verify { cryptoProvider.decryptMessageWithKeyPair(message.toByteArray(), publicKey, privateKey) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `decrypts ByteArray message with public key`() {
        val message = "9434dc98".asHexString().toByteArray()
        val publicKey = ByteArray(0)
        val privateKey = ByteArray(0)

        val decrypted = crypto.decryptMessageWithKeyPair(message, publicKey, privateKey)

        assertTrue(decrypted.isSuccess, "Expected decryption result to be a success")
        verify { cryptoProvider.convertEd25519PublicKeyToCurve25519(publicKey) }
        verify { cryptoProvider.convertEd25519PrivateKeyToCurve25519(privateKey) }
        verify { cryptoProvider.decryptMessageWithKeyPair(message, publicKey, privateKey) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `returns failure result if failed to decrypt string message with key pair`() {
        cryptoProvider.shouldFail = true

        val message = "message"
        val publicKey = ByteArray(0)
        val privateKey = ByteArray(0)

        val decrypted = crypto.decryptMessageWithKeyPair(message, publicKey, privateKey)

        assertTrue(decrypted.isFailure, "Expected decryption result to be a failure")
    }

    @Test
    fun `returns failure result if failed to decrypt HexString message with key pair`() {
        cryptoProvider.shouldFail = true

        val message = "9434dc98".asHexString()
        val publicKey = ByteArray(0)
        val privateKey = ByteArray(0)

        val decrypted = crypto.decryptMessageWithKeyPair(message, publicKey, privateKey)

        assertTrue(decrypted.isFailure, "Expected decryption result to be a failure")
    }

    @Test
    fun `returns failure result if failed to decrypt ByteArray message with key pair`() {
        cryptoProvider.shouldFail = true

        val message = "9434dc98".asHexString().toByteArray()
        val publicKey = ByteArray(0)
        val privateKey = ByteArray(0)

        val decrypted = crypto.decryptMessageWithKeyPair(message, publicKey, privateKey)

        assertTrue(decrypted.isFailure, "Expected decryption result to be a failure")
    }

    @Test
    fun `encrypts string message with shared key`() {
        val message = "message"
        val sharedKey = ByteArray(0)

        val encrypted = crypto.encryptMessageWithSharedKey(message, sharedKey)

        assertTrue(encrypted.isSuccess, "Expected encryption result to be a success")
        verify { cryptoProvider.encryptMessageWithSharedKey(message.encodeToByteArray(), sharedKey) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `encrypts HexString message with shared key`() {
        val message = "9434dc98".asHexString()
        val sharedKey = ByteArray(0)

        val encrypted = crypto.encryptMessageWithSharedKey(message, sharedKey)

        assertTrue(encrypted.isSuccess, "Expected encryption result to be a success")
        verify { cryptoProvider.encryptMessageWithSharedKey(message.toByteArray(), sharedKey) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `encrypts ByteArray message with shared key`() {
        val message = "9434dc98".asHexString().toByteArray()
        val sharedKey = ByteArray(0)

        val encrypted = crypto.encryptMessageWithSharedKey(message, sharedKey)

        assertTrue(encrypted.isSuccess, "Expected encryption result to be a success")
        verify { cryptoProvider.encryptMessageWithSharedKey(message, sharedKey) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `returns failure result if failed to encrypt string message with shared key`() {
        cryptoProvider.shouldFail = true

        val message = "message"
        val publicKey = ByteArray(0)

        val encrypted = crypto.encryptMessageWithSharedKey(message, publicKey)

        assertTrue(encrypted.isFailure, "Expected encryption result to be a failure")
    }

    @Test
    fun `returns failure result if failed to encrypt HexString message with shared key`() {
        cryptoProvider.shouldFail = true

        val message = "9434dc98".asHexString()
        val publicKey = ByteArray(0)

        val encrypted = crypto.encryptMessageWithSharedKey(message, publicKey)

        assertTrue(encrypted.isFailure, "Expected encryption result to be a failure")
    }

    @Test
    fun `returns failure result if failed to encrypt ByteArray message with shared key`() {
        cryptoProvider.shouldFail = true

        val message = "9434dc98".asHexString().toByteArray()
        val publicKey = ByteArray(0)

        val encrypted = crypto.encryptMessageWithSharedKey(message, publicKey)

        assertTrue(encrypted.isFailure, "Expected encryption result to be a failure")
    }

    @Test
    fun `decrypts string message with shared key`() {
        val message = "message"
        val sharedKey = ByteArray(0)

        val decrypted = crypto.decryptMessageWithSharedKey(message, sharedKey)

        assertTrue(decrypted.isSuccess, "Expected decryption result to be a success")
        verify { cryptoProvider.decryptMessageWithSharedKey(message.encodeToByteArray(), sharedKey) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `decrypts HexString message with shared key`() {
        val message = "9434dc98".asHexString()
        val sharedKey = ByteArray(0)

        val decrypted = crypto.decryptMessageWithSharedKey(message, sharedKey)

        assertTrue(decrypted.isSuccess, "Expected decryption result to be a success")
        verify { cryptoProvider.decryptMessageWithSharedKey(message.toByteArray(), sharedKey) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `decrypts ByteArray message with shared key`() {
        val message = "9434dc98".asHexString().toByteArray()
        val sharedKey = ByteArray(0)

        val decrypted = crypto.decryptMessageWithSharedKey(message, sharedKey)

        assertTrue(decrypted.isSuccess, "Expected decryption result to be a success")
        verify { cryptoProvider.decryptMessageWithSharedKey(message, sharedKey) }

        confirmVerified(cryptoProvider)
    }

    @Test
    fun `returns failure result if failed to decrypt string message with shared key`() {
        cryptoProvider.shouldFail = true

        val message = "message"
        val sharedKey = ByteArray(0)

        val decrypted = crypto.decryptMessageWithSharedKey(message, sharedKey)

        assertTrue(decrypted.isFailure, "Expected decryption result to be a failure")
    }

    @Test
    fun `returns failure result if failed to decrypt HexString message with shared key`() {
        cryptoProvider.shouldFail = true

        val message = "9434dc98".asHexString()
        val sharedKey = ByteArray(0)

        val decrypted = crypto.decryptMessageWithSharedKey(message, sharedKey)

        assertTrue(decrypted.isFailure, "Expected decryption result to be a failure")
    }

    @Test
    fun `returns failure result if failed to decrypt ByteArray message with shared key`() {
        cryptoProvider.shouldFail = true

        val message = "9434dc98".asHexString().toByteArray()
        val sharedKey = ByteArray(0)

        val decrypted = crypto.decryptMessageWithSharedKey(message, sharedKey)

        assertTrue(decrypted.isFailure, "Expected decryption result to be a failure")
    }
}