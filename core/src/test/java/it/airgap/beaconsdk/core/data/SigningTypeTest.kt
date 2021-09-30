package it.airgap.beaconsdk.core.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

internal class SigningTypeTest {

    @Test
    fun `is deserialized from string`() {
        expectedWithJsonValues()
            .map { Json.decodeFromString<SigningType>(it.second) to it.first }
            .forEach { assertEquals(it.second, it.first) }
    }

    @Test
    fun `serializes to string`() {
        expectedWithJsonValues()
            .map { Json.encodeToString(it.first) to it.second }
            .forEach { assertEquals(it.second, it.first) }
    }

    private fun expectedWithJsonValues(): List<Pair<SigningType, String>> = listOf(
        SigningType.Raw to "\"raw\"",
    )
}