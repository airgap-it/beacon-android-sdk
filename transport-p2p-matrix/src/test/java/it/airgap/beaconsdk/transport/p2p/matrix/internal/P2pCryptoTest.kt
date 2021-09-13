package it.airgap.beaconsdk.transport.p2p.matrix.internal

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import io.mockk.verify
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.core.internal.crypto.data.SessionKeyPair
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.utils.failure
import it.airgap.beaconsdk.core.internal.utils.toHexString
import mockTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class P2pCryptoTest {

    @MockK
    private lateinit var crypto: Crypto

    private lateinit var p2pCrypto: P2pCrypto

    private val app: BeaconApplication = BeaconApplication(
        KeyPair(byteArrayOf(0), byteArrayOf(0)),
        "mockApp",
        "mockAppIcon",
        "mockAppUrl",
    )

    private val currentTimestamp: Long = 300_000
    private val sessionKeyPair = SessionKeyPair(byteArrayOf(0, 0), byteArrayOf(1, 1))

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockTime(currentTimeMillis = currentTimestamp)

        every { crypto.hashKey(any<ByteArray>()) } answers { Result.success(firstArg()) }
        every { crypto.hash(any<String>(), any()) } answers { Result.success(firstArg<String>().encodeToByteArray()) }

        every { crypto.signMessageDetached(any<ByteArray>(), any()) } answers { Result.success(firstArg()) }

        every { crypto.encryptMessageWithPublicKey(any<String>(), any()) } answers { Result.success(firstArg<String>().encodeToByteArray()) }

        every { crypto.createClientSessionKeyPair(any(), any()) } returns Result.success(sessionKeyPair)
        every { crypto.createServerSessionKeyPair(any(), any()) } returns Result.success(sessionKeyPair)
        every { crypto.encryptMessageWithSharedKey(any<String>(), any()) } answers { Result.success(firstArg<String>().encodeToByteArray()) }
        every { crypto.decryptMessageWithSharedKey(any<String>(), any()) } answers { Result.success(firstArg<String>().encodeToByteArray()) }

        p2pCrypto = P2pCrypto(app, crypto)
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `creates user ID`() {
        val userId = p2pCrypto.userId().getOrThrow()

        assertEquals(app.keyPair.publicKey.toHexString().asString(), userId)
        verify(exactly = 1) { crypto.hashKey(app.keyPair.publicKey) }
    }

    @Test
    fun `returns failure result if user ID could not be created`() {
        every { crypto.hashKey(any<ByteArray>()) } returns Result.failure()

        val userId = p2pCrypto.userId()

        assertTrue(userId.isFailure, "Expected userId result to be a failure")
        verify(exactly = 1) { crypto.hashKey(app.keyPair.publicKey) }
    }

    @Test
    fun `creates password`() {
        val password = p2pCrypto.password().getOrThrow()
        val loginDigest = "login:${currentTimestamp / 1000 / (5 * 60)}"

        assertEquals(
            "ed:${loginDigest.encodeToByteArray().toHexString().asString()}:${app.keyPair.publicKey.toHexString().asString()}",
            password,
        )
        verify(exactly = 1) { crypto.hash(loginDigest, 32) }
        verify(exactly = 1) { crypto.signMessageDetached(loginDigest.encodeToByteArray(), app.keyPair.privateKey) }
    }

    @Test
    fun `returns failure result if password could not be created`() {
        every { crypto.hash(any<String>(), any()) } returns Result.failure()

        val password = p2pCrypto.password()

        assertTrue(password.isFailure, "Expected password result to be a failure")
    }

    @Test
    fun `creates device ID`() {
        val deviceId = p2pCrypto.deviceId().getOrThrow()

        assertEquals(app.keyPair.publicKey.toHexString().asString(), deviceId)
    }

    @Test
    fun `encrypts pairing payload with given key`() {
        val publicKey = byteArrayOf(1)
        val pairingPayload = "pairingPayload"

        val encrypted = p2pCrypto.encryptPairingPayload(publicKey, pairingPayload).getOrThrow()

        assertContentEquals(pairingPayload.encodeToByteArray(), encrypted)
        verify(exactly = 1) { crypto.encryptMessageWithPublicKey(pairingPayload, publicKey) }
    }

    @Test
    fun `returns failure result if pairing payload could not be encrypted`() {
        every { crypto.encryptMessageWithPublicKey(any<String>(), any()) } returns Result.failure()

        val publicKey = byteArrayOf(1)
        val pairingPayload = "pairingPayload"

        val encrypted = p2pCrypto.encryptPairingPayload(publicKey, pairingPayload)

        assertTrue(encrypted.isFailure, "Expected encrypted result to be a failure")
    }

    @Test
    fun `encrypts message with shared key`() {
        val publicKey = byteArrayOf(1)
        val message = "message"

        val encrypted = p2pCrypto.encrypt(publicKey, message).getOrThrow()

        assertEquals(message.encodeToByteArray().toHexString().asString(), encrypted)
        verify(exactly = 1) { crypto.createClientSessionKeyPair(publicKey, app.keyPair.privateKey) }
        verify(exactly = 1) { crypto.encryptMessageWithSharedKey(message, sessionKeyPair.tx) }
    }

    @Test
    fun `returns failure result if message could not be encrypted with shared key`() {
        every { crypto.encryptMessageWithSharedKey(any<String>(), any()) } returns Result.failure()

        val publicKey = byteArrayOf(1)
        val message = "message"

        val encrypted = p2pCrypto.encrypt(publicKey, message)

        assertTrue(encrypted.isFailure, "Expected encrypted result to be a failure")
        verify(exactly = 1) { crypto.createClientSessionKeyPair(publicKey, app.keyPair.privateKey) }
        verify(exactly = 1) { crypto.encryptMessageWithSharedKey(message, sessionKeyPair.tx) }
    }

    @Test
    fun `decrypts message with shared key`() {
        val publicKey = byteArrayOf(1)
        val message = "message"

        val decrypted = p2pCrypto.decrypt(publicKey, message).getOrThrow()

        assertEquals(message, decrypted)
        verify(exactly = 1) { crypto.createServerSessionKeyPair(publicKey, app.keyPair.privateKey) }
        verify(exactly = 1) { crypto.decryptMessageWithSharedKey(message, sessionKeyPair.rx) }
    }

    @Test
    fun `returns failure result if message could not be decrypted with shared key`() {
        every { crypto.decryptMessageWithSharedKey(any<String>(), any()) } returns Result.failure()

        val publicKey = byteArrayOf(1)
        val message = "message"

        val decrypted = p2pCrypto.decrypt(publicKey, message)

        assertTrue(decrypted.isFailure, "Expected decrypted result to be a failure")
        verify(exactly = 1) { crypto.createServerSessionKeyPair(publicKey, app.keyPair.privateKey) }
        verify(exactly = 1) { crypto.decryptMessageWithSharedKey(message, sessionKeyPair.rx) }
    }

    @Test
    fun `creates client session key pair once`() {
        val publicKey = byteArrayOf(1)
        val message = "message"

        p2pCrypto.encrypt(publicKey, message).getOrThrow()
        p2pCrypto.encrypt(publicKey, message).getOrThrow()

        verify(exactly = 1) { crypto.createClientSessionKeyPair(publicKey, app.keyPair.privateKey) }
        verify(exactly = 2) { crypto.encryptMessageWithSharedKey(message, sessionKeyPair.tx) }
    }

    @Test
    fun `creates server session key pair once`() {
        val publicKey = byteArrayOf(1)
        val message = "message"

        p2pCrypto.decrypt(publicKey, message).getOrThrow()
        p2pCrypto.decrypt(publicKey, message).getOrThrow()

        verify(exactly = 1) { crypto.createServerSessionKeyPair(publicKey, app.keyPair.privateKey) }
        verify(exactly = 2) { crypto.decryptMessageWithSharedKey(message, sessionKeyPair.rx) }
    }
}