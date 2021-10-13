package it.airgap.beaconsdk.core.internal.utils

import io.mockk.verify
import it.airgap.beaconsdk.core.exception.BeaconException
import mockLog
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class TryTest {

    @Before
    fun setup() {
        mockLog()
    }

    @Test
    fun `executes block returning Result and returns flatten result if no exception was thrown`() {
        val result = runCatchingFlat { Result.success() }

        assertTrue(result.isSuccess, "Expected result to be a success")
    }

    @Test
    fun `executes block returning Result and returns failure result on error`() {
        val result = runCatchingFlat<Unit> { failWith() }

        assertTrue(result.isFailure, "Expected result to be a failure")
    }

    @Test
    fun `executes block and returns result on success`() {
        val result = tryLog("TAG") {}

        assertNotNull(result, "Expected result not to be null")
        verify(exactly = 0) { logError(any(), any()) }
    }

    @Test
    fun `executes block and logs exception on error returning null as result`() {
        val tag = "TAG"
        val result = tryLog<Unit>(tag) { failWith() }

        assertNull(result, "Expected result to be null")
        verify(exactly = 1) { logError(tag, match { it is BeaconException }) }
    }

    @Test
    fun `executes block n times or until succeeded`() {
        val n = 3

        var counterFail = 0
        val resultFail = runCatchingRepeat(n) {
           counterFail++
            throw Exception()
        }

        var counterSuccess = 0
        val resultSuccess = runCatchingRepeat(n) {
            counterSuccess++
            if (counterSuccess != n - 1) throw Exception()
        }

        assertTrue(resultFail.isFailure)
        assertEquals(n, counterFail)

        assertTrue(resultSuccess.isSuccess)
        assertEquals(n - 1, counterSuccess)
    }

    @Test
    fun `executes block n times or until succeeded and flattens result`() {
        val n = 3

        var counterFail = 0
        val resultFail = runCatchingFlatRepeat(n) {
            counterFail++
            Result.failure<Unit>()
        }

        var counterSuccess = 0
        val resultSuccess = runCatchingFlatRepeat(n) {
            counterSuccess++
            if (counterSuccess == n - 1) Result.success() else Result.failure()
        }

        assertTrue(resultFail.isFailure)
        assertEquals(n, counterFail)

        assertTrue(resultSuccess.isSuccess)
        assertEquals(n - 1, counterSuccess)
    }

}