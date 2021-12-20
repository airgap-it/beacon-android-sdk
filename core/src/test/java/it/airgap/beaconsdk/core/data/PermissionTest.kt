package it.airgap.beaconsdk.core.data

import fromValues
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import mockBeaconSdk
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class PermissionTest {

    @MockK
    private lateinit var dependencyRegistry: DependencyRegistry

    @MockK
    private lateinit var blockchainRegistry: BlockchainRegistry

    private lateinit var mockBlockchain: MockBlockchain

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockBeaconSdk(dependencyRegistry = dependencyRegistry)

        mockBlockchain = MockBlockchain()

        every { dependencyRegistry.blockchainRegistry } returns blockchainRegistry

        every { blockchainRegistry.get(any()) } returns mockBlockchain
        every { blockchainRegistry.getOrNull(any()) } returns mockBlockchain
    }

    @Test
    fun `is deserialized from JSON`() {
        listOf(
            expectedWithJson(),
            expectedWithJson(threshold = Threshold("amount", "frame")),
            expectedWithJson(includeNulls = true),
        ).map {
            Json.decodeFromString<Permission>(it.second) to it.first
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    @Test
    fun `serializes to JSON`() {
        listOf(expectedWithJson(), expectedWithJson(threshold = Threshold("amount", "frame")))
            .map {
                Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(it.first)) to
                    Json.decodeFromString(JsonObject.serializer(), it.second) }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    private fun expectedWithJson(
        blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
        accountId: String = "accountId",
        senderId: String = "senderId",
        appMetadata: AppMetadata = AppMetadata(senderId, "name"),
        connectedAt: Long = 0,
        threshold: Threshold? = null,
        includeNulls: Boolean = false,
    ): Pair<Permission, String> {
        val values = mapOf(
            "threshold" to threshold?.let { Json.encodeToJsonElement(it) },
            "blockchainIdentifier" to blockchainIdentifier,
            "accountId" to accountId,
            "senderId" to senderId,
            "appMetadata" to Json.encodeToJsonElement(appMetadata),
            "connectedAt" to connectedAt,
        )

        val json = JsonObject.fromValues(values, includeNulls).toString()

        return MockPermission(
            blockchainIdentifier,
            accountId,
            senderId,
            appMetadata,
            connectedAt,
            threshold,
        ) to json
    }
}