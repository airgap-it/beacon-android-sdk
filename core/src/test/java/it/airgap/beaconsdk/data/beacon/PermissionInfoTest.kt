package it.airgap.beaconsdk.data.beacon

import fromValues
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.Test
import kotlin.test.assertEquals

internal class PermissionInfoTest {
    @Test
    fun `is deserialized from JSON`() {
        listOf(
            expectedWithJson(),
            expectedWithJson(threshold = Threshold("amount", "frame")),
            expectedWithJson(includeNulls = true),
        ).map {
            Json.decodeFromString<PermissionInfo>(it.second) to it.first
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    @Test
    fun `serializes to JSON`() {
        listOf(expectedWithJson(), expectedWithJson(threshold = Threshold("amount", "frame")))
            .map { Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(it.first)) to
                    Json.decodeFromString(JsonObject.serializer(), it.second) }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    private fun expectedWithJson(
        accountIdentifier: String = "accountIdentifier",
        address: String = "address",
        network: Network = Network.Custom(),
        scopes: List<PermissionScope> = emptyList(),
        senderId: String = "senderId",
        appMetadata: AppMetadata = AppMetadata(senderId, "name"),
        publicKey: String = "publicKey",
        connectedAt: Long = 0,
        threshold: Threshold? = null,
        includeNulls: Boolean = false,
    ): Pair<PermissionInfo, String> {
        val values = mapOf(
            "address" to address,
            "network" to Json.encodeToJsonElement(network),
            "scopes" to Json.encodeToJsonElement(scopes),
            "threshold" to threshold?.let { Json.encodeToJsonElement(it) },
            "accountIdentifier" to accountIdentifier,
            "senderId" to senderId,
            "appMetadata" to Json.encodeToJsonElement(appMetadata),
            "publicKey" to publicKey,
            "connectedAt" to connectedAt,
        )

        val json = JsonObject.fromValues(values, includeNulls).toString()

        return PermissionInfo(
            accountIdentifier,
            address,
            network,
            scopes,
            senderId,
            appMetadata,
            publicKey,
            connectedAt,
            threshold,
        ) to json
    }
}