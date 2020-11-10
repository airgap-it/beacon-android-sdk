package it.airgap.beaconsdk.internal.message.beacon.v1

import fromValues
import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.internal.message.v1.V1AppMetadata
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test
import kotlin.test.assertEquals

internal class V1AppMetadataTest {

    @Test
    fun `is deserialized from JSON`() {
        listOf(expectedWithJson(), expectedWithJson(includeNulls = true), expectedWithJson(icon = "icon"))
            .map { Json.decodeFromString<V1AppMetadata>(it.second) to it.first }
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
            .map { V1AppMetadata.fromAppMetadata(it.second) to it.first }
            .forEach { assertEquals(it.second, it.first) }
    }

    @Test
    fun `converts to AppMetadata`() {
        listOf(expectedWithAppMetadata(), expectedWithAppMetadata(icon = "icon"))
            .map { it.first.toAppMetadata() to it.second }
            .forEach { assertEquals(it.second, it.first) }
    }

    private fun expectedWithJson(
        beaconId: String = "beaconId",
        name: String = "name",
        icon: String? = null,
        includeNulls: Boolean = false,
    ): Pair<V1AppMetadata, String> {
        val values = mapOf(
            "beaconId" to beaconId,
            "name" to name,
            "icon" to icon,
        )

        val json = JsonObject.fromValues(values, includeNulls).toString()

        return V1AppMetadata(beaconId, name, icon) to json
    }

    private fun expectedWithAppMetadata(
        beaconId: String = "beaconId",
        name: String = "name",
        icon: String? = null,
    ): Pair<V1AppMetadata, AppMetadata> =
        V1AppMetadata(beaconId, name, icon) to AppMetadata(beaconId, name, icon)
}