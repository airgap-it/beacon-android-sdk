package it.airgap.beaconsdk.client.wallet.compat

import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.client.wallet.BeaconWalletClient
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import mockBeaconSdk
import org.junit.Before
import org.junit.Test
import java.security.KeyStore
import kotlin.test.assertEquals

internal class BeaconWalletClientBuilderTest {

    private lateinit var testDeferred: CompletableDeferred<Unit>

    private lateinit var beaconApp: BeaconSdk

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

        beaconApp = mockBeaconSdk(beaconId = beaconId, dependencyRegistry = dependencyRegistry)

        testDeferred = CompletableDeferred()
    }

    @Test
    fun `builds BeaconWalletClient with default settings`() {
        val defaultConnections = emptyList<Connection>()

        var client: BeaconWalletClient? = null
        val callback = spyk<BuildCallback>(object : BuildCallback {
            override fun onSuccess(beaconWalletClient: BeaconWalletClient) {
                client = beaconWalletClient
                testDeferred.complete(Unit)
            }

            override fun onError(error: Throwable) = Unit
        })

        BeaconWalletClient.Builder(appName).apply {
            support(*blockchains.toTypedArray())
        }.build(callback)

        runBlocking { testDeferred.await() }

        assertEquals(appName, client?.name)
        assertEquals(beaconId, client?.beaconId)

        coVerify(exactly = 1) {
            beaconApp.init(
                BeaconApplication.Partial(appName, null, null),
                BeaconConfiguration(ignoreUnsupportedBlockchains = false),
                blockchains,
                ofType(SharedPreferencesStorage::class),
                ofType(SharedPreferencesSecureStorage::class)
            )
        }
        verify(exactly = 1) { dependencyRegistry.connectionController(defaultConnections) }
        verify(exactly = 1) { callback.onSuccess(any()) }
        verify(exactly = 0) { callback.onError(any()) }
        verify(exactly = 0) { callback.onCancel() }

        confirmVerified(callback)
    }

    @Test
    fun `builds BeaconWalletClient with custom matrix nodes`() {
        val mockP2pFactory = mockkClass(P2pClient.Factory::class)
        val customConnections = listOf(P2P(mockP2pFactory))

        var client: BeaconWalletClient? = null
        val callback = spyk<BuildCallback>(object : BuildCallback {
            override fun onSuccess(beaconWalletClient: BeaconWalletClient) {
                client = beaconWalletClient
                testDeferred.complete(Unit)
            }

            override fun onError(error: Throwable) = Unit
        })

        BeaconWalletClient.Builder(appName).apply {
            support(*blockchains.toTypedArray())
            use(*customConnections.toTypedArray())
        }.build(callback)

        runBlocking { testDeferred.await() }

        assertEquals(appName, client!!.name)
        assertEquals(beaconId, client!!.beaconId)

        coVerify(exactly = 1) {
            beaconApp.init(
                BeaconApplication.Partial(appName, null, null),
                BeaconConfiguration(ignoreUnsupportedBlockchains = false),
                blockchains,
                ofType(SharedPreferencesStorage::class),
                ofType(SharedPreferencesSecureStorage::class)
            )
        }
        verify(exactly = 1) { dependencyRegistry.connectionController(customConnections) }
        verify(exactly = 1) { callback.onSuccess(any()) }
        verify(exactly = 0) { callback.onError(any()) }
        verify(exactly = 0) { callback.onCancel() }

        confirmVerified(callback)
    }
}