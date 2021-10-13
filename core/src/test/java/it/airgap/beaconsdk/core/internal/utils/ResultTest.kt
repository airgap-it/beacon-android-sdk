package it.airgap.beaconsdk.core.internal.utils

import it.airgap.beaconsdk.core.exception.BeaconException
import org.junit.Test
import kotlin.test.assertEquals

internal class ResultTest {

    @Test
    fun `maps value if is Success`() {
        val string = "value"
        val integer = string.length

        val success: Result<String> = Result.success(string)
        val transformed = success.map { integer }

        assertEquals(Result.success(integer), transformed)
    }

    @Test
    fun `keeps error on map value if is Failure`() {
        val error = IllegalStateException()

        val failure: Result<String> = Result.failure(error)
        val transformed = failure.map { it.length }

        assertEquals(Result.failure(error), transformed)
    }

    @Test
    fun `keeps value on map error if is Success`() {
        val value = "value"

        val success: Result<String> = Result.success(value)
        val transformed = success.mapException { Exception(it.cause) }

        assertEquals(Result.success(value), transformed)
    }

    @Test
    fun `maps error if is Failure`() {
        val cause = IllegalStateException()
        val error = BeaconException.from(cause)

        val failure: Result<String> = Result.failure(error)
        val transformed = failure.mapException { it.cause!! }

        assertEquals(Result.failure(cause), transformed)
    }

    @Test
    fun `maps and flattens result if Success`() {
        val value = "value"

        val success: Result<String> = Result.success(value)
        val transformed = success.flatMap { Result.success(it) }

        assertEquals(success, transformed)
    }

    @Test
    fun `keeps error on flat map value if Success`() {
        val error = IllegalStateException()

        val failure: Result<String> = Result.failure(error)
        val transformed = failure.flatMap { Result.success(it) }

        assertEquals(Result.failure(error), transformed)
    }
}