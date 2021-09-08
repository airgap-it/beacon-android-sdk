package it.airgap.beaconsdk.core.compat.client

import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.client.BeaconClient
import it.airgap.beaconsdk.core.data.beacon.P2P
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.BeaconSdk
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.SharedPreferencesSecureStorage
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.SharedPreferencesStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import mockBeaconApp
import org.junit.Before
import org.junit.Test
import java.security.KeyStore
import kotlin.test.assertEquals

internal class BeaconClientBuilderTest {

    private lateinit var testDeferred: CompletableDeferred<Unit>

    private lateinit var beaconApp: BeaconSdk

    @MockK(relaxed = true)
    private lateinit var dependencyRegistry: DependencyRegistry

    private val appName: String = "mockApp"
    private val beaconId: String = "beaconId"

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockkStatic(KeyStore::class)
        every { KeyStore.getInstance(any()) } returns mockk(relaxed = true)

        beaconApp = mockBeaconApp(beaconId = beaconId, dependencyRegistry = dependencyRegistry)

        testDeferred = CompletableDeferred()
    }

    @Test
    fun `builds BeaconClient with default settings`() {
        val defaultConnections = listOf(P2P(BeaconConfiguration.defaultRelayServers))

        var client: BeaconClient? = null
        val callback = spyk<BuildCallback>(object : BuildCallback {
            override fun onSuccess(beaconClient: BeaconClient) {
                client = beaconClient
                testDeferred.complete(Unit)
            }

            override fun onError(error: Throwable) = Unit
        })

        BeaconClient.Builder(appName).build(callback)

        runBlocking { testDeferred.await() }

        assertEquals(appName, client?.name)
        assertEquals(beaconId, client?.beaconId)

        coVerify(exactly = 1) { beaconApp.init(appName, null, null, ofType(SharedPreferencesStorage::class), ofType(SharedPreferencesSecureStorage::class)) }
        verify(exactly = 1) { dependencyRegistry.connectionController(defaultConnections) }
        verify(exactly = 1) { callback.onSuccess(any()) }
        verify(exactly = 0) { callback.onError(any()) }
        verify(exactly = 0) { callback.onCancel() }

        confirmVerified(callback)
    }

    @Test
    fun `builds BeaconClient with custom matrix nodes`() {
        val customConnections = listOf(P2P(listOf("node#1", "node#2")))

        var client: BeaconClient? = null
        val callback = spyk<BuildCallback>(object : BuildCallback {
            override fun onSuccess(beaconClient: BeaconClient) {
                client = beaconClient
                testDeferred.complete(Unit)
            }

            override fun onError(error: Throwable) = Unit
        })

        BeaconClient.Builder(appName).apply {
            connections = customConnections
        }.build(callback)

        runBlocking { testDeferred.await() }

        assertEquals(appName, client!!.name)
        assertEquals(beaconId, client!!.beaconId)

        coVerify(exactly = 1) { beaconApp.init(appName, null, null, ofType(SharedPreferencesStorage::class), ofType(SharedPreferencesSecureStorage::class)) }
        verify(exactly = 1) { dependencyRegistry.connectionController(customConnections) }
        verify(exactly = 1) { callback.onSuccess(any()) }
        verify(exactly = 0) { callback.onError(any()) }
        verify(exactly = 0) { callback.onCancel() }

        confirmVerified(callback)
    }
}