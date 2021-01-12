package it.airgap.beaconsdk.internal.transport.p2p.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test
import kotlin.test.assertEquals

internal class P2pHandshakeInfoTest {

    @Test
    fun `is deserialized from JSON`() {
        listOf(expectedWithJson())
            .map { Json.decodeFromString<P2pPairingResponse>(it.second) to it.first }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `serializes to JSON`() {
        listOf(expectedWithJson())
            .map {
                Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(it.first)) to
                        Json.decodeFromString(JsonObject.serializer(), it.second)
            }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    private fun expectedWithJson(
        id: String = "id",
        type: String = "type",
        name: String = "name",
        version: String = "version",
        publicKey: String = "publicKey",
        relayServer: String = "relayServer",
    ): Pair<P2pPairingResponse, String> =
        P2pPairingResponse(id, type, name, version, publicKey, relayServer) to """
            {
                "id": "$id",
                "type": "$type",
                "name": "$name",
                "version": "$version",
                "publicKey": "$publicKey",
                "relayServer": "$relayServer"
            }
        """.trimIndent()
}