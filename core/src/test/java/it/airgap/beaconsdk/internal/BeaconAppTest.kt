package it.airgap.beaconsdk.internal

import org.junit.Test
import kotlin.test.assertFailsWith

internal class BeaconAppTest {

    @Test
    fun `fails to return instance before initialized`() {
        assertFailsWith(IllegalStateException::class) {
            BeaconApp.instance
        }
    }
}