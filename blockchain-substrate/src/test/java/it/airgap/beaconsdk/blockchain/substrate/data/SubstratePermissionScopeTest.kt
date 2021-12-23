package it.airgap.beaconsdk.blockchain.substrate.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.junit.Test
import kotlin.test.assertEquals

internal class SubstratePermissionScopeTest {

    @Test
    fun `is deserialized from string`() {
        expectedWithJsonValues()
            .map { Json.decodeFromString<SubstratePermission.Scope>(it.second) to it.first }
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
            .map { Json.decodeFromString<List<SubstratePermission.Scope>>(it.second) to it.first }
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

    private fun expectedWithJsonValues(): List<Pair<SubstratePermission.Scope, String>> = listOf(
        SubstratePermission.Scope.Transfer to "\"transfer\"",
        SubstratePermission.Scope.SignRaw to "\"sign_raw\"",
        SubstratePermission.Scope.SignString to "\"sign_string\"",
    )

    private fun expectedWithJsonArray(): List<Pair<List<SubstratePermission.Scope>, String>> = listOf(
        listOf(SubstratePermission.Scope.Transfer, SubstratePermission.Scope.SignRaw, SubstratePermission.Scope.SignString) to
                """
                    [
                        "transfer",
                        "sign_raw",
                        "sign_string"
                    ]
                """.trimIndent(),
    )
}