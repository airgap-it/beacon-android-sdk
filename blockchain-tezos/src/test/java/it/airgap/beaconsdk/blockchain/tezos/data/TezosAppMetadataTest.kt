package it.airgap.beaconsdk.blockchain.tezos.data

import fromValues
import it.airgap.beaconsdk.blockchain.tezos.Tezos
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test
import kotlin.test.assertEquals

internal class TezosAppMetadataTest {
    @Test
    fun `is deserialized from JSON`() {
        listOf(
            expectedWithJson(),
            expectedWithJson(blockchainIdentifier = null),
            expectedWithJson(icon = "icon"),
            expectedWithJson(includeNulls = true),
        ).map {
            println(it.second)
            Json.decodeFromString<TezosAppMetadata>(it.second) to it.first
        }.forEach {
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

    private fun expectedWithJson(
        blockchainIdentifier: String? = Tezos.IDENTIFIER,
        senderId: String = "senderId",
        name: String = "name",
        icon: String? = null,
        includeNulls: Boolean = false
    ): Pair<TezosAppMetadata, String> {
        val values = mapOf(
            "blockchainIdentifier" to blockchainIdentifier,
            "senderId" to senderId,
            "name" to name,
            "icon" to icon,
        )

        val json = JsonObject.fromValues(values, includeNulls).toString()

        return TezosAppMetadata(senderId, name, icon) to json
    }
}