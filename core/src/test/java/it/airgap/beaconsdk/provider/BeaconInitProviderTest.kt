package it.airgap.beaconsdk.provider

import android.content.Context
import io.mockk.*
import it.airgap.beaconsdk.internal.BeaconApp
import it.airgap.beaconsdk.internal.utils.logInfo
import mockLog
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class BeaconInitProviderTest {

    private lateinit var beaconInitProvider: BeaconInitProvider

    @Before
    fun setup() {
        mockLog()

        beaconInitProvider = spyk(BeaconInitProvider())
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `creates BeaconApp on created`() {
        val context = mockk<Context>(relaxed = true)
        every { beaconInitProvider.context } returns context
        beaconInitProvider.onCreate()

        verify { BeaconApp.create(context) }

        // BeaconInitProvider is run by the system outside the user control
        // and it's crucial to properly inform the user of the result of its actions
        verify { logInfo(BeaconInitProvider.TAG, "BeaconApp created") }
    }

    @Test
    fun `fails to create BeaconApp if no context`() {
        every { beaconInitProvider.context } returns null
        beaconInitProvider.onCreate()

        // BeaconInitProvider is run by the system outside the user control
        // and it's crucial to properly inform the user of the result of its actions
        verify { logInfo(BeaconInitProvider.TAG, "BeaconApp could not be created") }
    }
}