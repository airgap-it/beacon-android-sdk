package it.airgap.beaconsdk.blockchain.tezos.data

import fromValues
import it.airgap.beaconsdk.blockchain.tezos.Tezos
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.Test
import kotlin.test.assertEquals

internal class TezosPermissionTest {

    @Test
    fun `is deserialized from JSON`() {
        listOf(
            expectedWithJson(),
            expectedWithJson(blockchainIdentifier = null),
            expectedWithJson(includeNulls = true),
        ).map {
            Json.decodeFromString<TezosPermission>(it.second) to it.first
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    @Test
    fun `serializes to JSON`() {
        listOf(expectedWithJson())
            .map {
                Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(it.first)) to
                    Json.decodeFromString(JsonObject.serializer(), it.second) }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    private fun expectedWithJson(
        blockchainIdentifier: String? = Tezos.IDENTIFIER,
        accountId: String = "accountId",
        senderId: String = "senderId",
        connectedAt: Long = 0,
        address: String = "address",
        publicKey: String = "publicKey",
        network: TezosNetwork = TezosNetwork.Custom(),
        appMetadata: TezosAppMetadata = TezosAppMetadata(senderId, "name"),
        scopes: List<TezosPermission.Scope> = emptyList(),
        includeNulls: Boolean = false,
    ): Pair<TezosPermission, String> {
        val values = mapOf(
            "blockchainIdentifier" to blockchainIdentifier,
            "accountId" to accountId,
            "senderId" to senderId,
            "connectedAt" to connectedAt,
            "address" to address,
            "publicKey" to publicKey,
            "network" to Json.encodeToJsonElement(network),
            "appMetadata" to Json.encodeToJsonElement(appMetadata),
            "scopes" to Json.encodeToJsonElement(scopes),
        )

        val json = JsonObject.fromValues(values, includeNulls).toString()

        return TezosPermission(
            accountId,
            senderId,
            connectedAt,
            address,
            publicKey,
            network,
            appMetadata,
            scopes,
        ) to json
    }
}