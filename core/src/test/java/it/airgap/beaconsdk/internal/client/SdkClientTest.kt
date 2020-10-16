package it.airgap.beaconsdk.internal.client

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.verify
import it.airgap.beaconsdk.internal.BeaconConfig
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.compat.storage.ExtendedStorage
import it.airgap.beaconsdk.internal.utils.internalSuccess
import it.airgap.beaconsdk.storage.MockBeaconStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SdkClientTest {
    @MockK
    private lateinit var crypto: Crypto

    private lateinit var storage: ExtendedStorage
    private lateinit var sdkClient: SdkClient

    private val versionName: String = "1.2.3"
    private val secretSeed: String = "seed"
    private val privateKey: ByteArray = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
    private val publicKey: ByteArray = byteArrayOf(10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockkObject(BeaconConfig)
        every { BeaconConfig.versionName } returns versionName

        every { crypto.generateRandomSeed() } returns internalSuccess(secretSeed)
        every { crypto.getKeyPairFromSeed(any()) } returns internalSuccess(KeyPair(privateKey, publicKey))

        storage = ExtendedStorage(MockBeaconStorage())
        sdkClient = SdkClient(storage, crypto)
    }

    @Test
    fun `initializes with keyPair and beaconId`() {
        runBlocking { sdkClient.init() }

        assertEquals(privateKey, sdkClient.keyPair?.privateKey)
        assertEquals(publicKey, sdkClient.keyPair?.publicKey)

        assertEquals("0a0b0c0d0e0f1011121314", sdkClient.beaconId)
    }

    @Test
    fun `saves version name and secret seed in storage`() {
        runBlocking { sdkClient.init() }

        val sdkVersion = runBlocking { storage.getSdkVersion() }
        val sdkSecretSeed = runBlocking { storage.getSdkSecretSeed() }

        assertEquals(versionName, sdkVersion)
        assertEquals(secretSeed, sdkSecretSeed)
    }

    @Test
    fun `loads secret seed from storage if already saved`() {
        val savedSecretSeed = "saved seed"

        runBlocking {
            storage.setSdkSecretSeed(savedSecretSeed)
            sdkClient.init()
        }

        val storageSecretSeed = runBlocking { storage.getSdkSecretSeed() }

        assertEquals(savedSecretSeed, storageSecretSeed)

        verify(exactly = 0) { crypto.generateRandomSeed() }
        verify { crypto.getKeyPairFromSeed(savedSecretSeed) }
    }
}