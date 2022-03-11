package it.airgap.beaconsdk.blockchain.substrate.data

import it.airgap.beaconsdk.blockchain.substrate.Substrate
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.Test
import kotlin.test.assertEquals

internal class SubstratePermissionTest {

    @Test
    fun `is deserialized from JSON`() {
        listOf(
            expectedWithJson(),
        ).map {
            Json.decodeFromString<SubstratePermission>(it.second) to it.first
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
        blockchainIdentifier: String = Substrate.IDENTIFIER,
        accountId: String = "accountId",
        senderId: String = "senderId",
        connectedAt: Long = 0,
        appMetadata: SubstrateAppMetadata = SubstrateAppMetadata(senderId, "name"),
        account: SubstrateAccount = SubstrateAccount(accountId, SubstrateNetwork("genesisHash"), "publicKey", "address"),
        scopes: List<SubstratePermission.Scope> = emptyList(),
    ): Pair<SubstratePermission, String> =
        SubstratePermission(
            accountId,
            senderId,
            connectedAt,
            appMetadata,
            scopes,
            account,
        ) to """
            {
                "blockchainIdentifier":  "$blockchainIdentifier",
                "accountId":  "$accountId",
                "senderId":  "$senderId",
                "connectedAt":  $connectedAt,
                "appMetadata":  ${Json.encodeToString(appMetadata)},
                "scopes":  ${Json.encodeToJsonElement(scopes)},
                "account":  ${Json.encodeToString(account)}
            }
        """.trimIndent()
}