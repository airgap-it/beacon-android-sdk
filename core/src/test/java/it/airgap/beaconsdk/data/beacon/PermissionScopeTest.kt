package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.junit.Test
import kotlin.test.assertEquals

internal class PermissionScopeTest {

    @Test
    fun `is deserialized from string`() {
        expectedWithJsonValues()
            .map { Json.decodeFromString<PermissionScope>(it.second) to it.first }
            .forEach { assertEquals(it.second, it.first) }
    }

    @Test
    fun `serializes to string`() {
        expectedWithJsonValues()
            .map { Json.encodeToString(it.first) to it.second }
            .forEach { assertEquals(it.second, it.first) }
    }

    @Test
    fun `is deserialized from list of strings`() {
        expectedWithJsonArray()
            .map { Json.decodeFromString<List<PermissionScope>>(it.second) to it.first }
            .forEach { assertEquals(it.second, it.first) }
    }

    @Test
    fun `serializes to list of string`() {
        expectedWithJsonArray()
            .map { Json.decodeFromString(JsonArray.serializer(), Json.encodeToString(it.first)) to
                    Json.decodeFromString(JsonArray.serializer(), it.second) }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    private fun expectedWithJsonValues(): List<Pair<PermissionScope, String>> = listOf(
        PermissionScope.Sign to "\"sign\"",
        PermissionScope.OperationRequest to "\"operation_request\"",
        PermissionScope.Threshold to "\"threshold\"",
    )

    private fun expectedWithJsonArray(): List<Pair<List<PermissionScope>, String>> = listOf(
        listOf(PermissionScope.Sign, PermissionScope.OperationRequest, PermissionScope.Threshold) to
                """
                    [
                        "sign", 
                        "operation_request", 
                        "threshold"
                    ]
                """.trimIndent(),
    )
}