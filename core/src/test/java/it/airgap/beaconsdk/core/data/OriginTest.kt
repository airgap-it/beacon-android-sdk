package it.airgap.beaconsdk.core.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test
import kotlin.test.assertEquals

internal class OriginTest {
    @Test
    fun `is deserialized from JSON`() {
        expectedWithJsonStrings()
            .map { Json.decodeFromString<Origin>(it.second) to it.first }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `serializes to JSON`() {
        expectedWithJsonStrings()
            .map {
                Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(it.first)) to
                        Json.decodeFromString(JsonObject.serializer(), it.second)
            }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `creates origin from peer`() {
        expectedWithPeers()
            .map { it.first to Origin.forPeer(it.second) }
            .forEach { assertEquals(it.first, it.second) }
    }

    private fun expectedWithJsonStrings(id: String = "id"): List<Pair<Origin, String>> = listOf(
        Origin.Website(id) to json("website", id),
        Origin.Extension(id) to json("extension", id),
        Origin.P2P(id) to json("p2p", id),
    )

    private fun expectedWithPeers(publicKey: String = "publicKey"): List<Pair<Origin, Peer>> = listOf(
        Origin.P2P(publicKey) to P2pPeer(name = "name", publicKey = publicKey, relayServer = "relayServer"),
    )

    private fun json(type: String, id: String): String =
        """
            {
                "type": "$type",
                "id": "$id"
            }
        """.trimIndent()
}