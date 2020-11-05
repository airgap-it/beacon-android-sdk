package it.airgap.beaconsdk.internal.utils

import it.airgap.beaconsdk.exception.BeaconException
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class ErrorTest {

    @Test
    fun `fails with internal error`() {
        assertFailsWith<BeaconException.Internal> { failWith() }
    }

    @Test
    fun `fails with internal error and specified message`() {
        val message = "message"
        val exception = assertFailsWith<BeaconException.Internal> { failWith(message) }

        assertEquals(message, exception.message)
    }

    @Test
    fun `fails with internal error and specified message and cause`() {
        val message = "message"
        val cause = IllegalStateException()

        val exception = assertFailsWith<BeaconException.Internal> { failWith(message, cause) }

        assertEquals(message, exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun `fails with specified cause`() {
        val cause = IllegalStateException()

        val exception = assertFailsWith<IllegalStateException> { failWith(cause = cause) }

        assertEquals(cause, exception)
    }
}