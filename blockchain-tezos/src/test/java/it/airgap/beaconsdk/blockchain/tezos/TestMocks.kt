package it.airgap.beaconsdk.blockchain.tezos
import io.mockk.every
import io.mockk.mockkStatic
import it.airgap.beaconsdk.core.internal.utils.currentTimestamp

// -- static --

internal fun mockTime(currentTimeMillis: Long = 1) {
    mockkStatic("it.airgap.beaconsdk.core.internal.utils.TimeKt")
    every { currentTimestamp() } returns currentTimeMillis
}
