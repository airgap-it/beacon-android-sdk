package it.airgap.beaconsdk.client.wallet

import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.data.beacon.Connection
import it.airgap.beaconsdk.core.data.beacon.P2P
import it.airgap.beaconsdk.core.internal.BeaconSdk
import it.airgap.beaconsdk.core.internal.chain.Chain
import it.airgap.beaconsdk.core.internal.chain.MockChain
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.SharedPreferencesSecureStorage
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.SharedPreferencesStorage
import it.airgap.beaconsdk.core.internal.transport.p2p.P2pClient
import kotlinx.coroutines.runBlocking
import mockBeaconSdk
import org.junit.Before
import org.junit.Test
import java.security.KeyStore
import kotlin.test.assertEquals

internal class BeaconWalletClientBuilderTest {

    private lateinit var beaconSdk: BeaconSdk

    @MockK(relaxed = true)
    private lateinit var dependencyRegistry: DependencyRegistry

    private val appName: String = "mockApp"
    private val chains: List<Chain.Factory<*>> = listOf(MockChain.Factory())
    private val beaconId: String = "beaconId"

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockkStatic(KeyStore::class)
        every { KeyStore.getInstance(any()) } returns mockk(relaxed = true)

        beaconSdk = mockBeaconSdk(beaconId = beaconId, dependencyRegistry = dependencyRegistry)
    }

    @Test
    fun `builds BeaconWalletClient with default settings`() {
        runBlocking {
            val beaconWalletClient = BeaconWalletClient.Builder(appName, chains).build()
            val defaultConnections = emptyList<Connection>()

            assertEquals(appName, beaconWalletClient.name)
            assertEquals(beaconId, beaconWalletClient.beaconId)

            coVerify(exactly = 1) { beaconSdk.init(appName, null, null, chains, ofType(SharedPreferencesStorage::class), ofType(SharedPreferencesSecureStorage::class)) }
            verify(exactly = 1) { dependencyRegistry.connectionController(defaultConnections) }
        }
    }

    @Test
    fun `builds BeaconWalletClient with custom connections`() {
        runBlocking {
            val mockP2pFactory = mockkClass(P2pClient.Factory::class)
            val customConnections = listOf(P2P(mockP2pFactory))

            val beaconWalletClient = BeaconWalletClient.Builder(appName, chains).apply {
                addConnections(*customConnections.toTypedArray())
            }.build()

            assertEquals(appName, beaconWalletClient.name)
            assertEquals(beaconId, beaconWalletClient.beaconId)

            coVerify(exactly = 1) { beaconSdk.init(appName, null, null, chains, ofType(SharedPreferencesStorage::class), ofType(SharedPreferencesSecureStorage::class)) }
            verify(exactly = 1) { dependencyRegistry.connectionController(customConnections) }
        }
    }

    @Test
    fun `builds BeaconWalletClient with default settings when used as builder function`() {
        runBlocking {
            val beaconWalletClient = BeaconWalletClient(appName, chains)
            val defaultConnections = emptyList<Connection>()

            assertEquals(appName, beaconWalletClient.name)
            assertEquals(beaconId, beaconWalletClient.beaconId)

            coVerify(exactly = 1) { beaconSdk.init(appName, null, null, chains, ofType(SharedPreferencesStorage::class), ofType(SharedPreferencesSecureStorage::class)) }
            verify(exactly = 1) { dependencyRegistry.connectionController(defaultConnections) }
        }
    }

    @Test
    fun `builds BeaconWalletClient with custom connections as builder function`() {
        runBlocking {
            val mockP2pFactory = mockkClass(P2pClient.Factory::class)
            val customConnections = listOf(P2P(mockP2pFactory))

            val beaconWalletClient = BeaconWalletClient(appName, chains) { addConnections(*customConnections.toTypedArray()) }

            assertEquals(appName, beaconWalletClient.name)
            assertEquals(beaconId, beaconWalletClient.beaconId)

            coVerify(exactly = 1) { beaconSdk.init(appName, null, null, chains, ofType(SharedPreferencesStorage::class), ofType(SharedPreferencesSecureStorage::class)) }
            verify(exactly = 1) { dependencyRegistry.connectionController(customConnections) }
        }
    }
}