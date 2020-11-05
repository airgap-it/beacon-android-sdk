package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test
import kotlin.test.assertEquals

internal class ThresholdTest {
    @Test
    fun `is deserialized from JSON`() {
        listOf(expectedWithJson())
            .map { Json.decodeFromString<Threshold>(it.second) to it.first }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `serializes to JSON`() {
        listOf(expectedWithJson())
            .map {
                Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(it.first)) to
                        Json.decodeFromString(JsonObject.serializer(), it.second)
            }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    private fun expectedWithJson(amount: String = "amount", timeframe: String = "timeframe"): Pair<Threshold, String> =
        Threshold(amount, timeframe) to """
            {
                "amount": "$amount",
                "timeframe": "$timeframe"
            }
        """.trimIndent()
}