package it.airgap.beaconsdk.compat.client

import io.mockk.*
import it.airgap.beaconsdk.client.BeaconClient
import it.airgap.beaconsdk.internal.BeaconApp
import it.airgap.beaconsdk.internal.BeaconConfig
import it.airgap.beaconsdk.internal.storage.SharedPreferencesStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import mockBeaconApp
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class BeaconClientBuilderTest {

    private lateinit var testDeferred: CompletableDeferred<Unit>

    private lateinit var beaconApp: BeaconApp

    private val appName: String = "mockApp"
    private val beaconId: String = "beaconId"

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        beaconApp = mockBeaconApp(beaconId = beaconId)

        testDeferred = CompletableDeferred()
    }

    @Test
    fun `builds BeaconClient with default settings`() {
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

        coVerify(exactly = 1) { beaconApp.init(appName, BeaconConfig.defaultRelayServers, ofType(
            SharedPreferencesStorage::class)) }
        verify(exactly = 1) { callback.onSuccess(any()) }
        verify(exactly = 0) { callback.onError(any()) }

        confirmVerified(callback)
    }

    @Test
    fun `builds BeaconClient with custom matrix nodes`() {
        val customNodes = listOf("node#1", "node#2")

        var client: BeaconClient? = null
        val callback = spyk<BuildCallback>(object : BuildCallback {
            override fun onSuccess(beaconClient: BeaconClient) {
                client = beaconClient
                testDeferred.complete(Unit)
            }

            override fun onError(error: Throwable) = Unit
        })

        BeaconClient.Builder(appName).apply {
            matrixNodes(customNodes)
        }.build(callback)

        runBlocking { testDeferred.await() }

        assertEquals(appName, client!!.name)
        assertEquals(beaconId, client!!.beaconId)

        coVerify(exactly = 1) { beaconApp.init(appName, customNodes, ofType(SharedPreferencesStorage::class)) }
        verify(exactly = 1) { callback.onSuccess(any()) }
        verify(exactly = 0) { callback.onError(any()) }

        confirmVerified(callback)
    }
}