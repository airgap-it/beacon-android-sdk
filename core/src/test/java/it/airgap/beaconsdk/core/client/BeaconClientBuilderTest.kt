package it.airgap.beaconsdk.core.client

import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.data.beacon.P2P
import it.airgap.beaconsdk.core.internal.BeaconSdk
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.SharedPreferencesSecureStorage
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.SharedPreferencesStorage
import kotlinx.coroutines.runBlocking
import mockBeaconApp
import org.junit.Before
import org.junit.Test
import java.security.KeyStore
import kotlin.test.assertEquals

internal class BeaconClientBuilderTest {

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
    }

    @Test
    fun `builds BeaconClient with default settings`() {
        runBlocking {
            val beaconClient = BeaconClient.Builder(appName).build()
            val defaultConnections = listOf(P2P(BeaconConfiguration.defaultRelayServers))

            assertEquals(appName, beaconClient.name)
            assertEquals(beaconId, beaconClient.beaconId)

            coVerify(exactly = 1) { beaconApp.init(appName, null, null, ofType(SharedPreferencesStorage::class), ofType(SharedPreferencesSecureStorage::class)) }
            verify(exactly = 1) { dependencyRegistry.connectionController(defaultConnections) }
        }
    }

    @Test
    fun `builds BeaconClient with custom matrix nodes`() {
        runBlocking {
            val customConnections = listOf(P2P(listOf("node#1", "node#2")))

            val beaconClient = BeaconClient.Builder(appName).apply {
                connections = customConnections
            }.build()

            assertEquals(appName, beaconClient.name)
            assertEquals(beaconId, beaconClient.beaconId)

            coVerify(exactly = 1) { beaconApp.init(appName, null, null, ofType(SharedPreferencesStorage::class), ofType(SharedPreferencesSecureStorage::class)) }
            verify(exactly = 1) { dependencyRegistry.connectionController(customConnections) }
        }
    }

    @Test
    fun `builds BeaconClient with default settings when used as builder function`() {
        runBlocking {
            val beaconClient = BeaconClient(appName)
            val defaultConnections = listOf(P2P(BeaconConfiguration.defaultRelayServers))

            assertEquals(appName, beaconClient.name)
            assertEquals(beaconId, beaconClient.beaconId)

            coVerify(exactly = 1) { beaconApp.init(appName, null, null, ofType(SharedPreferencesStorage::class), ofType(SharedPreferencesSecureStorage::class)) }
            verify(exactly = 1) { dependencyRegistry.connectionController(defaultConnections) }
        }
    }

    @Test
    fun `builds BeaconClient with custom matrix nodes when used as builder function`() {
        runBlocking {
            val customConnections = listOf(P2P(listOf("node#1", "node#2")))

            val beaconClient = BeaconClient(appName) { connections = customConnections }

            assertEquals(appName, beaconClient.name)
            assertEquals(beaconId, beaconClient.beaconId)

            coVerify(exactly = 1) { beaconApp.init(appName, null, null, ofType(SharedPreferencesStorage::class), ofType(SharedPreferencesSecureStorage::class)) }
            verify(exactly = 1) { dependencyRegistry.connectionController(customConnections) }
        }
    }
}