package it.airgap.beaconsdk.core.data.beacon

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
            .map { Json.decodeFromString<Permission.Scope>(it.second) to it.first }
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
            .map { Json.decodeFromString<List<Permission.Scope>>(it.second) to it.first }
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

    private fun expectedWithJsonValues(): List<Pair<Permission.Scope, String>> = listOf(
        Permission.Scope.Sign to "\"sign\"",
        Permission.Scope.OperationRequest to "\"operation_request\"",
        Permission.Scope.Threshold to "\"threshold\"",
    )

    private fun expectedWithJsonArray(): List<Pair<List<Permission.Scope>, String>> = listOf(
        listOf(Permission.Scope.Sign, Permission.Scope.OperationRequest, Permission.Scope.Threshold) to
                """
                    [
                        "sign", 
                        "operation_request", 
                        "threshold"
                    ]
                """.trimIndent(),
    )
}