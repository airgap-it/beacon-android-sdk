package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test
import kotlin.test.assertEquals

internal class P2pPeerInfoTest {
    @Test
    fun `is deserialized from JSON`() {
        listOf(
            expectedWithJson(),
            expectedWithJson(includeNulls = true),
            expectedWithJson(version = "version"),
            expectedWithJson(icon = "icon"),
            expectedWithJson(appUrl = "appUrl"),
            expectedWithJson(version = "version", icon = "icon"),
            expectedWithJson(version = "version", appUrl = "appUrl"),
            expectedWithJson(version = "version", icon = "icon", appUrl = "appUrl"),
        ).map {
            Json.decodeFromString<P2pPeerInfo>(it.second) to it.first
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    @Test
    fun `serializes to JSON`() {
        listOf(
            expectedWithJson(),
            expectedWithJson(version = "version"),
            expectedWithJson(icon = "icon"),
            expectedWithJson(appUrl = "appUrl"),
            expectedWithJson(version = "version", icon = "icon"),
            expectedWithJson(version = "version", appUrl = "appUrl"),
            expectedWithJson(version = "version", icon = "icon", appUrl = "appUrl"),
        ).map {
            Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(it.first)) to
                    Json.decodeFromString(JsonObject.serializer(), it.second)
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    private fun expectedWithJson(
        name: String = "name",
        publicKey: String = "publicKey",
        relayServer: String = "relayServer",
        version: String? = null,
        icon: String? = null,
        appUrl: String? = null,
        includeNulls: Boolean = false,
    ): Pair<P2pPeerInfo, String> =
        P2pPeerInfo(name, publicKey, relayServer, version, icon, appUrl) to
                json(name, publicKey, relayServer, version, icon, appUrl, includeNulls)

    private fun json(
        name: String = "name",
        publicKey: String = "publicKey",
        relayServer: String = "relayServer",
        version: String? = null,
        icon: String? = null,
        appUrl: String? = null,
        includeNulls: Boolean = false,
    ): String =
        when {
            !includeNulls && version == null && icon == null && appUrl == null -> {
                """
                    {
                        "name": "$name",
                        "publicKey": "$publicKey",
                        "relayServer": "$relayServer"
                    }
                """.trimIndent()
            }
            !includeNulls && icon == null && appUrl == null -> {
                """
                    {
                        "name": "$name",
                        "publicKey": "$publicKey",
                        "relayServer": "$relayServer",
                        "version": "$version"
                    }
                """.trimIndent()
            }
            !includeNulls && version == null && appUrl == null -> {
                """
                    {
                        "name": "$name",
                        "publicKey": "$publicKey",
                        "relayServer": "$relayServer",
                        "icon": "$icon"
                    }
                """.trimIndent()
            }
            !includeNulls && version == null && icon == null -> {
                """
                    {
                        "name": "$name",
                        "publicKey": "$publicKey",
                        "relayServer": "$relayServer",
                        "appUrl": "$appUrl"
                    }
                """.trimIndent()
            }
            !includeNulls && appUrl == null -> {
                """
                    {
                        "name": "$name",
                        "publicKey": "$publicKey",
                        "relayServer": "$relayServer",
                        "version": "$version",
                        "icon": "$icon"
                    }
                """.trimIndent()
            }

            !includeNulls && icon == null -> {
                """
                    {
                        "name": "$name",
                        "publicKey": "$publicKey",
                        "relayServer": "$relayServer",
                        "version": "$version",
                        "appUrl": "$appUrl"
                    }
                """.trimIndent()
            }
            !includeNulls && version == null -> {
                """
                    {
                        "name": "$name",
                        "publicKey": "$publicKey",
                        "relayServer": "$relayServer",
                        "icon": "$icon",
                        "appUrl": "$appUrl"
                    }
                """.trimIndent()
            }
            else -> {
                """
                    {
                        "name": "$name",
                        "publicKey": "$publicKey",
                        "relayServer": "$relayServer",
                        "version": ${Json.encodeToString(version)},
                        "icon": ${Json.encodeToString(icon)},
                        "appUrl": ${Json.encodeToString(appUrl)}
                    }
                """.trimIndent()
            }
        }
}