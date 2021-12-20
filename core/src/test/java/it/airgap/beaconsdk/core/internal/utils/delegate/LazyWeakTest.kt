package it.airgap.beaconsdk.core.internal.utils.delegate

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class LazyWeakTest {

    @Test
    fun `initializes value`() {
        var initCounter = 0
        val value by lazyWeak {
            initCounter++
            TestReference()
        }

        val h1 = value.hashCode()
        System.gc()
        val h2 = value.hashCode()

        assertNotEquals(h1, h2, "Expected lazy weak values hash codes to be different.")
        assertEquals(2, initCounter)
    }

    @Test
    fun `keeps value reference`() {
        var initCounter = 0
        val value by lazyWeak {
            initCounter++
            TestReference()
        }

        val v = value

        val h1 = value.hashCode()
        System.gc()
        val h2 = value.hashCode()

        assertEquals(h1, h2, "Expected lazy weak values hash codes to be the same.")
        assertEquals(1, initCounter)
    }

    private class TestReference
}