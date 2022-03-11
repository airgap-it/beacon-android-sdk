package it.airgap.beaconsdk.blockchain.substrate.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test
import kotlin.test.assertEquals

internal class SubstrateAccountTest {
    @Test
    fun `is deserialized from JSON`() {
        listOf(
            expectedWithJsonStrings(),
        ).map {
            Json.decodeFromString<SubstrateAccount>(it.second) to it.first
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    @Test
    fun `serializes to JSON`() {
        listOf(
            expectedWithJsonStrings(),
        ).map {
            Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(it.first)) to
                    Json.decodeFromString(JsonObject.serializer(), it.second)
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    private fun expectedWithJsonStrings(
        accountId: String = "accountId",
        network: SubstrateNetwork = SubstrateNetwork("genesisHash"),
        publicKey: String = "publicKey",
        address: String = "address",
    ): Pair<SubstrateAccount, String> =
        SubstrateAccount(accountId, network, publicKey, address) to """
            {
                "accountId": "$accountId",
                "network": ${Json.encodeToString(network)},
                "publicKey": "$publicKey",
                "address": "$address"
            }
        """.trimIndent()
}