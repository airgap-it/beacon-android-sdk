package it.airgap.beaconsdk.core.data

import fromValues
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test
import kotlin.test.assertEquals

internal class PeerTest {
    @Test
    fun `is deserialized from JSON`() {
        (expectedWithJson() + expectedWithJson(includeNulls = true))
            .map { Json.decodeFromString<Peer>(it.second) to it.first }
            .forEach { assertEquals(it.second, it.first) }
    }

    @Test
    fun `serializes to JSON`() {
        expectedWithJson()
            .map {
                Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(it.first)) to
                        Json.decodeFromString(JsonObject.serializer(), it.second)
            }.forEach {
                assertEquals(it.second, it.first)
            }
    }

    private fun expectedWithJson(
        id: String = "id",
        name: String = "name",
        publicKey: String = "publicKey",
        version: String = "version",
        includeNulls: Boolean = false,
    ): List<Pair<Peer, String>> = listOf(
        expectedP2pWithJson(id, name, publicKey, version, includeNulls = includeNulls),
        expectedP2pWithJson(id, name, publicKey, version, icon = "icon", includeNulls = includeNulls),
        expectedP2pWithJson(id, name, publicKey, version, appUrl = "appUrl", includeNulls = includeNulls),
        expectedP2pWithJson(id, name, publicKey, version, icon = "icon", appUrl = "appUrl", includeNulls = includeNulls),
    )

    private fun expectedP2pWithJson(
        id: String = "id",
        name: String = "name",
        publicKey: String = "publicKey",
        version: String = "version",
        relayServer: String = "relayServer",
        icon: String? = null,
        appUrl: String? = null,
        includeNulls: Boolean = false,
    ): Pair<P2pPeer, String> =
        P2pPeer(id, name, publicKey, relayServer, version, icon, appUrl) to
                p2pJson(id, name, publicKey, relayServer, version, icon, appUrl, includeNulls)

    private fun p2pJson(
        id: String = "id",
        name: String = "name",
        publicKey: String = "publicKey",
        relayServer: String = "relayServer",
        version: String = "version",
        icon: String? = null,
        appUrl: String? = null,
        includeNulls: Boolean = false,
    ): String {
        val values = mapOf(
            "type" to "p2p",
            "id" to id,
            "name" to name,
            "publicKey" to publicKey,
            "relayServer" to relayServer,
            "version" to version,
            "icon" to icon,
            "appUrl" to appUrl,
        )

        return JsonObject.fromValues(values, includeNulls).toString()
    }
}