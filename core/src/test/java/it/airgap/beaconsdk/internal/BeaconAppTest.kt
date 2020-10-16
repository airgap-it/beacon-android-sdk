package it.airgap.beaconsdk.internal

import it.airgap.beaconsdk.internal.utils.uninitializedMessage
import org.junit.Test
import kotlin.test.assertFailsWith

class BeaconAppTest {

    @Test
    fun `fails to return instance before initialized`() {
        assertFailsWith(IllegalStateException::class, uninitializedMessage(BeaconApp.TAG)) {
            BeaconApp.instance
        }
    }
}