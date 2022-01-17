package it.airgap.beaconsdk.blockchain.substrate.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test
import kotlin.test.assertEquals

internal class SubstrateRuntimeSpecTest {
    @Test
    fun `is deserialized from JSON`() {
        listOf(
            expectedWithJsonStrings(),
        ).map {
            Json.decodeFromString<SubstrateRuntimeSpec>(it.second) to it.first
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    @Test
    fun `serializes to JSON`() {
        listOf(
            expectedWithJsonStrings(),
        ).map {
            Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(it.first)) to
                    Json.decodeFromString(JsonObject.serializer(), it.second)
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    private fun expectedWithJsonStrings(
        runtimeVersion: String = "runtimeVersion",
        transactionVersion: String = "transactionVersion",
    ): Pair<SubstrateRuntimeSpec, String> =
        SubstrateRuntimeSpec(runtimeVersion, transactionVersion) to """
            {
                "runtimeVersion": "$runtimeVersion",
                "transactionVersion": "$transactionVersion"
            }
        """.trimIndent()
}