
import io.mockk.every
import io.mockk.mockkStatic
import it.airgap.beaconsdk.core.internal.utils.currentTimestamp
import it.airgap.beaconsdk.core.internal.utils.logDebug
import it.airgap.beaconsdk.core.internal.utils.logError
import it.airgap.beaconsdk.core.internal.utils.logInfo

// -- static --

internal fun mockLog() {
    mockkStatic("it.airgap.beaconsdk.core.internal.utils.LogKt")

    every { logInfo(any(), any()) } answers {
        println("[INFO] ${firstArg<String>()}: ${secondArg<String>()}")
    }

    every { logDebug(any(), any()) } answers {
        println("[DEBUG] ${firstArg<String>()}: ${secondArg<String>()}")
    }

    every { logError(any(), any()) } answers {
        println("[ERROR] ${firstArg<String>()}: ${secondArg<Throwable>()}")
    }
}

internal fun mockTime(currentTimeMillis: Long = 1) {
    mockkStatic("it.airgap.beaconsdk.core.internal.utils.TimeKt")
    every { currentTimestamp() } returns currentTimeMillis
}
