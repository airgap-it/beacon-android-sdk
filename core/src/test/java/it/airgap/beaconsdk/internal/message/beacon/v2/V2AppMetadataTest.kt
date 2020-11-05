package it.airgap.beaconsdk.internal.message.beacon.v2

import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.internal.message.v2.V2AppMetadata
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test
import kotlin.test.assertEquals

internal class V2AppMetadataTest {

    @Test
    fun `is deserialized from JSON`() {
        listOf(expectedWithJson(), expectedWithJson(includeNulls = true), expectedWithJson(icon = "icon"))
            .map { Json.decodeFromString<V2AppMetadata>(it.second) to it.first }
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
            .map { V2AppMetadata.fromAppMetadata(it.second) to it.first }
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
    ): Pair<V2AppMetadata, String> {
        val json = if (icon != null || includeNulls) {
            """
                {
                    "senderId": "$senderId",
                    "name": "$name",
                    "icon": ${Json.encodeToString(icon)}
                }
            """.trimIndent()
        } else {
            """
                {
                    "senderId": "$senderId",
                    "name": "$name"
                }
            """.trimIndent()
        }

        return V2AppMetadata(senderId, name, icon) to json
    }

    private fun expectedWithAppMetadata(
        senderId: String = "senderId",
        name: String = "name",
        icon: String? = null,
    ): Pair<V2AppMetadata, AppMetadata> =
        V2AppMetadata(senderId, name, icon) to AppMetadata(senderId, name, icon)
}