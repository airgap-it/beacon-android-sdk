package it.airgap.beaconsdk.internal.utils

import it.airgap.beaconsdk.exception.BeaconException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import kotlin.test.*

internal class InternalResultTest {

    @Test
    fun `checks if is Success`() {
        val success = Success()
        val failure = Failure<Unit>()

        assertTrue(success.isSuccess, "Expected Success to recognize itself as a success result")
        assertFalse(failure.isSuccess, "Expected Failure not to recognize itself as a success result")
    }

    @Test
    fun `checks if is Failure`() {
        val success = Success()
        val failure = Failure<Unit>()

        assertFalse(success.isFailure, "Expected Success not to recognize itself as a failure result")
        assertTrue(failure.isFailure, "Expected Failure to recognize itself as a failure result")
    }

    @Test
    fun `returns value if is Success when trying to get value`() {
        val value = "value"
        val success: InternalResult<String> = Success(value)

        assertEquals(value, success.value())
    }

    @Test
    fun `fails with error if is Failure when trying to get value`() {
        val error = IllegalStateException()
        val failure: InternalResult<String> = Failure(error)

        val exception = assertFailsWith<IllegalStateException> { failure.value() }
        assertEquals(error, exception)
    }

    @Test
    fun `returns value if is Success when trying to get nullable value`() {
        val value = "value"
        val success: InternalResult<String> = Success(value)

        assertEquals(value, success.valueOrNull())
    }

    @Test
    fun `returns null if is Failure when trying to get nullable value`() {
        val failure: InternalResult<String> = Failure()

        assertNull(failure.valueOrNull(), "Expected Failure result to return null value")
    }

    @Test
    fun `returns null if is Success when trying to get nullable error`() {
        val value = "value"
        val success: InternalResult<String> = Success(value)

        assertNull(success.errorOrNull(), "Expected Success result to return null error")
    }

    @Test
    fun `returns error if is Failure when trying to get nullable error`() {
        val error = IllegalStateException()
        val failure: InternalResult<String> = Failure(error)

        assertEquals(error, failure.errorOrNull())
    }

    @Test
    fun `maps value if is Success`() {
        val value = "value"

        val success: InternalResult<String> = Success(value)
        val transformed = success.map { it.length }

        assertEquals(Success(value.length), transformed)
    }

    @Test
    fun `keeps error on map value if is Failure`() {
        val error = IllegalStateException()

        val failure: InternalResult<String> = Failure(error)
        val transformed = failure.map { it.length }

        assertEquals(Failure(error), transformed)
    }

    @Test
    fun `suspend maps value if is Success`() {
        val value = "value"

        runBlockingTest {
            val success: InternalResult<String> = Success(value)
            val transformed = success.mapSuspend {
                delay(100)
                it.length
            }

            assertEquals(Success(value.length), transformed)
        }
    }

    @Test
    fun `keeps error on suspend map value if is Failure`() {
        val error = IllegalStateException()

        runBlockingTest {
            val failure: InternalResult<String> = Failure(error)
            val transformed = failure.mapSuspend {
                delay(100)
                it.length
            }

            assertEquals(Failure(error), transformed)
        }
    }

    @Test
    fun `keeps value on map error if is Success`() {
        val value = "value"

        val success: InternalResult<String> = Success(value)
        val transformed = success.mapError { Exception(it?.cause) }

        assertEquals(Success(value), transformed)
    }

    @Test
    fun `maps error if is Failure`() {
        val cause = IllegalStateException()
        val error = BeaconException(cause = cause)

        val failure: InternalResult<String> = Failure(error)
        val transformed = failure.mapError { it.cause!! }

        assertEquals(Failure(cause), transformed)
    }

    @Test
    fun `keeps value on suspend map error if is Success`() {
        val value = "value"

        runBlockingTest {
            val success: InternalResult<String> = Success(value)
            val transformed = success.mapErrorSuspend {
                delay(100)
                Exception(it.cause)
            }

            assertEquals(Success(value), transformed)
        }
    }

    @Test
    fun `suspend maps error if is Failure`() {
        val cause = IllegalStateException()
        val error = BeaconException(cause = cause)

        runBlockingTest {
            val failure: InternalResult<String> = Failure(error)
            val transformed = failure.mapErrorSuspend {
                delay(100)
                it.cause!!
            }

            assertEquals(Failure(cause), transformed)
        }
    }

    @Test
    fun `maps and flattens result if Success`() {
        val value = "value"

        val success: InternalResult<String> = Success(value)
        val transformed = success.flatMap { Success(it) }

        assertEquals(success, transformed)
    }

    @Test
    fun `keeps error on flat map value if Success`() {
        val error = IllegalStateException()

        val failure: InternalResult<String> = Failure(error)
        val transformed = failure.flatMap { Success(it) }

        assertEquals(Failure(error), transformed)
    }

    @Test
    fun `suspend maps and flattens result if Success`() {
        val value = "value"

        runBlockingTest {
            val success: InternalResult<String> = Success(value)
            val transformed = success.flatMapSuspend {
                delay(100)
                Success(it)
            }

            assertEquals(success, transformed)
        }
    }

    @Test
    fun `keeps error on suspend flat map value if Success`() {
        val error = IllegalStateException()

        runBlockingTest {
            val failure: InternalResult<String> = Failure(error)
            val transformed = failure.flatMapSuspend {
                delay(100)
                Success(it)
            }

            assertEquals(Failure(error), transformed)
        }
    }

    @Test
    fun `performs specified action on value if is Success`() {
        var performed = false
        val success: InternalResult<String> = Success("value")
        success.alsoIfSuccess { performed = true }

        assertTrue(performed, "Expected action to have been performed")
    }

    @Test
    fun `does not perform specified action on value if is Failure`() {
        var performed = false
        val failure: InternalResult<String> = Failure()
        failure.alsoIfSuccess { performed = true }

        assertFalse(performed, "Expected action not to have been performed")
    }
}