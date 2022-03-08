package it.airgap.beaconsdk.client.wallet

import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.data.P2P
import it.airgap.beaconsdk.core.internal.BeaconSdk
import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.SharedPreferencesSecureStorage
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.SharedPreferencesStorage
import it.airgap.beaconsdk.core.transport.p2p.P2pClient
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
    private val blockchains: List<Blockchain.Factory<*>> = listOf(MockBlockchain.Factory())
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
            val beaconWalletClient = BeaconWalletClient.Builder(appName).apply {
                support(*blockchains.toTypedArray())
            }.build()
            val defaultConnections = emptyList<Connection>()

            assertEquals(appName, beaconWalletClient.name)
            assertEquals(beaconId, beaconWalletClient.beaconId)

            coVerify(exactly = 1) {
                beaconSdk.init(
                    BeaconApplication.Partial(appName, null, null),
                    BeaconConfiguration(ignoreUnsupportedBlockchains = false),
                    blockchains,
                    ofType(SharedPreferencesStorage::class),
                    ofType(SharedPreferencesSecureStorage::class)
                )
            }
            verify(exactly = 1) { dependencyRegistry.connectionController(defaultConnections) }
        }
    }

    @Test
    fun `builds BeaconWalletClient with custom connections`() {
        runBlocking {
            val mockP2pFactory = mockkClass(P2pClient.Factory::class)
            val customConnections = listOf(P2P(mockP2pFactory))

            val beaconWalletClient = BeaconWalletClient.Builder(appName).apply {
                support(*blockchains.toTypedArray())
                use(*customConnections.toTypedArray())
            }.build()

            assertEquals(appName, beaconWalletClient.name)
            assertEquals(beaconId, beaconWalletClient.beaconId)

            coVerify(exactly = 1) {
                beaconSdk.init(
                    BeaconApplication.Partial(appName, null, null),
                    BeaconConfiguration(ignoreUnsupportedBlockchains = false),
                    blockchains,
                    ofType(SharedPreferencesStorage::class),
                    ofType(SharedPreferencesSecureStorage::class)
                )
            }
            verify(exactly = 1) { dependencyRegistry.connectionController(customConnections) }
        }
    }

    @Test
    fun `builds BeaconWalletClient with default settings when used as builder function`() {
        runBlocking {
            val beaconWalletClient = BeaconWalletClient(appName) {
                support(*blockchains.toTypedArray())
            }
            val defaultConnections = emptyList<Connection>()

            assertEquals(appName, beaconWalletClient.name)
            assertEquals(beaconId, beaconWalletClient.beaconId)

            coVerify(exactly = 1) {
                beaconSdk.init(
                    BeaconApplication.Partial(appName, null, null),
                    BeaconConfiguration(ignoreUnsupportedBlockchains = false),
                    blockchains,
                    ofType(SharedPreferencesStorage::class),
                    ofType(SharedPreferencesSecureStorage::class)
                )
            }
            verify(exactly = 1) { dependencyRegistry.connectionController(defaultConnections) }
        }
    }

    @Test
    fun `builds BeaconWalletClient with custom connections as builder function`() {
        runBlocking {
            val mockP2pFactory = mockkClass(P2pClient.Factory::class)
            val customConnections = listOf(P2P(mockP2pFactory))

            val beaconWalletClient = BeaconWalletClient(appName) {
                support(*blockchains.toTypedArray())
                use(*customConnections.toTypedArray())
            }

            assertEquals(appName, beaconWalletClient.name)
            assertEquals(beaconId, beaconWalletClient.beaconId)

            coVerify(exactly = 1) {
                beaconSdk.init(
                    BeaconApplication.Partial(appName, null, null),
                    BeaconConfiguration(ignoreUnsupportedBlockchains = false),
                    blockchains,
                    ofType(SharedPreferencesStorage::class),
                    ofType(SharedPreferencesSecureStorage::class)
                )
            }
            verify(exactly = 1) { dependencyRegistry.connectionController(customConnections) }
        }
    }
}