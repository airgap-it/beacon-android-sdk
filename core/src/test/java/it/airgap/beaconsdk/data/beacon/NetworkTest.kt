package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test
import kotlin.test.assertEquals

internal class NetworkTest {
    @Test
    fun `is deserialized from JSON`() {
        (
                expectedWithJsonStrings() +
                expectedWithJsonStrings(includeNulls = true) +
                expectedWithJsonStrings(name = "name") +
                expectedWithJsonStrings(rpcUrl = "rpcUrl") +
                expectedWithJsonStrings(name = "name", rpcUrl = "rpcUrl")
        ).map {
            Json.decodeFromString<Network>(it.second) to it.first
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    @Test
    fun `serializes to JSON`() {
        (
                expectedWithJsonStrings() +
                expectedWithJsonStrings(name = "name") +
                expectedWithJsonStrings(rpcUrl = "rpcUrl") +
                expectedWithJsonStrings(name = "name", rpcUrl = "rpcUrl")
        ).map {
            Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(it.first)) to
                    Json.decodeFromString(JsonObject.serializer(), it.second)
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    private fun expectedWithJsonStrings(
        name: String? = null,
        rpcUrl: String? = null,
        includeNulls: Boolean = false,
    ): List<Pair<Network, String>> = listOf(
        Network.Mainnet(name, rpcUrl) to json("mainnet", name, rpcUrl, includeNulls),
        Network.Carthagenet(name, rpcUrl) to json("carthagenet", name, rpcUrl, includeNulls),
        Network.Custom(name, rpcUrl) to json("custom", name, rpcUrl, includeNulls),
    )

    private fun json(
        identifier: String,
        name: String? = null,
        rpcUrl: String? = null,
        includeNulls: Boolean = false,
    ): String =
        when {
            !includeNulls && name == null && rpcUrl == null -> {
                """
                    {
                        "type": "$identifier"
                    }
                """.trimIndent()
            }
            !includeNulls && name == null -> {
                """
                    {
                        "type": "$identifier",
                        "rpcUrl": "$rpcUrl"
                    }
                """.trimIndent()
            }
            !includeNulls && rpcUrl == null -> {
                """
                    {
                        "type": "$identifier",
                        "name": "$name"
                    }
                """.trimIndent()
            }
            else -> {
                """
                    {
                        "type": "$identifier",
                        "name": ${Json.encodeToString(name)},
                        "rpcUrl": ${Json.encodeToString(rpcUrl)}
                    }
                """.trimIndent()
            }
        }
}