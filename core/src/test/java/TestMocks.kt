
import android.content.Context
import io.mockk.*
import it.airgap.beaconsdk.internal.BeaconApp
import it.airgap.beaconsdk.internal.di.DependencyRegistry
import it.airgap.beaconsdk.internal.utils.currentTimestamp
import it.airgap.beaconsdk.internal.utils.logDebug
import it.airgap.beaconsdk.internal.utils.logError
import it.airgap.beaconsdk.internal.utils.logInfo

// -- class --

internal fun mockBeaconApp(
    beaconId: String = "beaconId",
    dependencyRegistry: DependencyRegistry = mockk(relaxed = true),
): BeaconApp =
    mockkClass(BeaconApp::class).also {
        mockkObject(BeaconApp)
        every { BeaconApp.instance } returns it

        val contextMock = mockk<Context>(relaxed = true)

        coEvery { it.init(any(), any(), any(), any(), any()) } returns Unit

        every { it.applicationContext } returns contextMock
        every { it.beaconId } returns beaconId
        every { it.dependencyRegistry } returns dependencyRegistry
    }

// -- static --

internal fun mockLog() {
    mockkStatic("it.airgap.beaconsdk.internal.utils.LogKt")

    every { logInfo(any(), any()) } answers {
        println("[INFO] ${firstArg<String>()}: ${secondArg<String>()}")
        Unit
    }

    every { logDebug(any(), any()) } answers {
        println("[DEBUG] ${firstArg<String>()}: ${secondArg<String>()}")
        Unit
    }

    every { logError(any(), any()) } answers {
        println("[ERROR] ${firstArg<String>()}: ${secondArg<Throwable>()}")
        Unit
    }
}

internal fun mockTime(currentTimeMillis: Long = 1) {
    mockkStatic("it.airgap.beaconsdk.internal.utils.TimeKt")
    every { currentTimestamp() } returns currentTimeMillis
}
