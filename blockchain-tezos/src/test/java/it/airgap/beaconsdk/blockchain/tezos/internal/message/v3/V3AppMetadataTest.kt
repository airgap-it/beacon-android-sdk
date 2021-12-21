package it.airgap.beaconsdk.blockchain.tezos.internal.message.v3

import fromValues
import it.airgap.beaconsdk.blockchain.tezos.data.TezosAppMetadata
import it.airgap.beaconsdk.core.data.AppMetadata
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test
import kotlin.test.assertEquals

internal class V3AppMetadataTest {

    @Test
    fun `is deserialized from JSON`() {
        listOf(expectedWithJson(), expectedWithJson(includeNulls = true), expectedWithJson(icon = "icon"))
            .map { Json.decodeFromString<V3TezosAppMetadata>(it.second) to it.first }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `serializes to JSON`() {
        listOf(expectedWithJson(), expectedWithJson(icon = "icon"))
            .map { Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(it.first)) to
                    Json.decodeFromString(JsonObject.serializer(), it.second) }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `is created from AppMetadata`() {
        listOf(expectedWithAppMetadata(), expectedWithAppMetadata(icon = "icon"))
            .map { V3TezosAppMetadata.fromAppMetadata(it.second) to it.first }
            .forEach { assertEquals(it.second, it.first) }
    }

    @Test
    fun `converts to AppMetadata`() {
        listOf(expectedWithAppMetadata(), expectedWithAppMetadata(icon = "icon"))
            .map { it.first.toAppMetadata() to it.second }
            .forEach { assertEquals(it.second, it.first) }
    }

    private fun expectedWithJson(
        senderId: String = "senderId",
        name: String = "name",
        icon: String? = null,
        includeNulls: Boolean = false,
    ): Pair<V3TezosAppMetadata, String> {
        val values = mapOf(
            "senderId" to senderId,
            "name" to name,
            "icon" to icon,
        )

        val json = JsonObject.fromValues(values, includeNulls).toString()

        return V3TezosAppMetadata(senderId, name, icon) to json
    }

    private fun expectedWithAppMetadata(
        senderId: String = "senderId",
        name: String = "name",
        icon: String? = null,
    ): Pair<V3TezosAppMetadata, TezosAppMetadata> =
        V3TezosAppMetadata(senderId, name, icon) to TezosAppMetadata(senderId, name, icon)
}