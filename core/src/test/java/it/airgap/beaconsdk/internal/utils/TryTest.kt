package it.airgap.beaconsdk.internal.utils

import io.mockk.verify
import it.airgap.beaconsdk.exception.BeaconException
import mockLog
import org.junit.Before
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class TryTest {

    @Before
    fun setup() {
        mockLog()
    }

    @Test
    fun `executes block and returns result on success as InternalResult`() {
        val result = tryResult { Unit }

        assertTrue(result.isSuccess, "Expected result to be a success")
    }

    @Test
    fun `executes block and returns error on failure as InternalResult`() {
        val result = tryResult { failWith() }

        assertTrue(result.isFailure, "Expected result to be a failure")
    }

    @Test
    fun `executes block returning InternalResult and returns flatten result if no exception was thrown`() {
        val result = flatTryResult { Success() }

        assertTrue(result.isSuccess, "Expected result to be a success")
    }

    @Test
    fun `executes block returning InternalResult and returns failure result on error`() {
        val result = flatTryResult<Unit> { failWith() }

        assertTrue(result.isFailure, "Expected result to be a failure")
    }

    @Test
    fun `executes block and returns result on success`() {
        val result = tryLog("TAG") { Unit }

        assertNotNull(result, "Expected result not to be null")
        verify(exactly = 0) { logError(any(), any()) }
    }

    @Test
    fun `executes block and logs exception on error returning null as result`() {
        val tag = "TAG"
        val result = tryLog<Unit>(tag) { failWith() }

        assertNull(result, "Expected result to be null")
        verify(exactly = 1) { logError(tag, match { it is BeaconException.Internal }) }
    }

}