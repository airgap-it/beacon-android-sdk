package it.airgap.beaconsdk.core.internal.utils.delegate

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class DisposableTest {

    @Test
    fun `returns null if no value set`() {
        val value by disposable<String>()

        assertNull(value)
    }

    @Test
    fun `returns and removes its value`() {
        val testValue = "utils"

        var value by disposable<String>()
        value = testValue

        assertEquals(testValue, value)
        assertNull(value)
    }
}