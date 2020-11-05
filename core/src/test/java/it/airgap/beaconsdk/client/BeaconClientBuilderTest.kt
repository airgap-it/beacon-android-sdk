package it.airgap.beaconsdk.client

import io.mockk.MockKAnnotations
import io.mockk.coVerify
import it.airgap.beaconsdk.internal.BeaconApp
import it.airgap.beaconsdk.internal.BeaconConfig
import it.airgap.beaconsdk.internal.storage.SharedPreferencesStorage
import kotlinx.coroutines.runBlocking
import mockBeaconApp
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class BeaconClientBuilderTest {

    private lateinit var beaconApp: BeaconApp

    private val appName: String = "mockApp"
    private val beaconId: String = "beaconId"

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        beaconApp = mockBeaconApp(beaconId = beaconId)
    }

    @Test
    fun `builds BeaconClient with default settings`() {
        runBlocking {
            val beaconClient = BeaconClient.Builder(appName).build()

            assertEquals(appName, beaconClient.name)
            assertEquals(beaconId, beaconClient.beaconId)

            coVerify(exactly = 1) {
                beaconApp.init(appName, BeaconConfig.defaultRelayServers, ofType(
                    SharedPreferencesStorage::class))
            }
        }
    }

    @Test
    fun `builds BeaconClient with custom matrix nodes`() {
        runBlocking {
            val customNodes = listOf("node#1", "node#2")

            val beaconClient1 = BeaconClient.Builder(appName).apply {
                matrixNodes(*customNodes.toTypedArray())
            }.build()

            val beaconClient2 = BeaconClient.Builder(appName).apply {
                matrixNodes(customNodes)
            }.build()

            assertEquals(appName, beaconClient1.name)
            assertEquals(beaconId, beaconClient1.beaconId)

            assertEquals(appName, beaconClient2.name)
            assertEquals(beaconId, beaconClient2.beaconId)

            coVerify(exactly = 2) {
                beaconApp.init(appName, customNodes, ofType(SharedPreferencesStorage::class))
            }
        }
    }

    @Test
    fun `builds BeaconClient with default settings when used as builder function`() {
        runBlocking {
            val beaconClient = BeaconClient(appName)

            assertEquals(appName, beaconClient.name)
            assertEquals(beaconId, beaconClient.beaconId)

            coVerify(exactly = 1) {
                beaconApp.init(appName, BeaconConfig.defaultRelayServers, ofType(
                    SharedPreferencesStorage::class))
            }
        }
    }

    @Test
    fun `builds BeaconClient with custom matrix nodes when used as builder function`() {
        runBlocking {
            val customNodes = listOf("node#1", "node#2")

            val beaconClient1 = BeaconClient(appName) { matrixNodes(*customNodes.toTypedArray()) }
            val beaconClient2 = BeaconClient(appName) { matrixNodes(customNodes) }

            assertEquals(appName, beaconClient1.name)
            assertEquals(beaconId, beaconClient1.beaconId)

            assertEquals(appName, beaconClient2.name)
            assertEquals(beaconId, beaconClient2.beaconId)

            coVerify(exactly = 2) {
                beaconApp.init(appName, customNodes, ofType(SharedPreferencesStorage::class))
            }
        }
    }
}