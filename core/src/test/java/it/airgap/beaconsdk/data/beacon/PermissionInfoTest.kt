package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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
        accountIdentifier: String = "accountIdentifeir",
        address: String = "address",
        network: Network = Network.Custom(),
        scopes: List<PermissionScope> = emptyList(),
        senderId: String = "senderId",
        appMetadata: AppMetadata = AppMetadata(senderId, "name"),
        website: String = "website",
        publicKey: String = "publicKey",
        connectedAt: Long = 0,
        threshold: Threshold? = null,
        includeNulls: Boolean = false,
    ): Pair<PermissionInfo, String> {
        val json = if (threshold != null || includeNulls) {
            """
                {
                    "address": "$address",
                    "network": ${Json.encodeToString(network)},
                    "scopes": ${Json.encodeToString(scopes)},
                    "threshold": ${Json.encodeToString(threshold)},
                    "accountIdentifier": "$accountIdentifier",
                    "senderId": "$senderId",
                    "appMetadata": ${Json.encodeToString(appMetadata)},
                    "website": "$website",
                    "publicKey": "$publicKey",
                    "connectedAt": $connectedAt
                }
            """.trimIndent()
        } else {
            """
                {
                    "address": "$address",
                    "network": ${Json.encodeToString(network)},
                    "scopes": ${Json.encodeToString(scopes)},
                    "accountIdentifier": "$accountIdentifier",
                    "senderId": "$senderId",
                    "appMetadata": ${Json.encodeToString(appMetadata)},
                    "website": "$website",
                    "publicKey": "$publicKey",
                    "connectedAt": $connectedAt
                }
            """.trimIndent()
        }

        return PermissionInfo(
            accountIdentifier,
            address,
            network,
            scopes,
            senderId,
            appMetadata,
            website,
            publicKey,
            connectedAt,
            threshold,
        ) to json
    }
}