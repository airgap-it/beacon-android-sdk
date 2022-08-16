package it.airgap.beaconsdk.core.data

import fromValues
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.compat.CoreCompat
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.serializer.contextualJson
import it.airgap.beaconsdk.core.scope.BeaconScope
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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
    private lateinit var json: Json

    private val beaconScope: BeaconScope = BeaconScope.Global

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockBeaconSdk(dependencyRegistry = dependencyRegistry)

        mockBlockchain = MockBlockchain()

        every { dependencyRegistry.blockchainRegistry } returns blockchainRegistry

        every { blockchainRegistry.get(any()) } returns mockBlockchain
        every { blockchainRegistry.getOrNull(any()) } returns mockBlockchain

        json = contextualJson(dependencyRegistry.blockchainRegistry, CoreCompat(beaconScope))
    }

    @Test
    fun `is deserialized from JSON`() {
        listOf(
            expectedWithJson(),
            expectedWithJson(includeNulls = true),
        ).map {
            json.decodeFromString<Permission>(it.second) to it.first
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    @Test
    fun `serializes to JSON`() {
        listOf(expectedWithJson())
            .map {
                json.decodeFromString(JsonObject.serializer(), json.encodeToString(it.first)) to
                    json.decodeFromString(JsonObject.serializer(), it.second) }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    private fun expectedWithJson(
        blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
        accountId: String = "accountId",
        senderId: String = "senderId",
        connectedAt: Long = 0,
        includeNulls: Boolean = false,
    ): Pair<Permission, String> {
        val values = mapOf(
            "blockchainIdentifier" to blockchainIdentifier,
            "accountId" to accountId,
            "senderId" to senderId,
            "connectedAt" to connectedAt,
        )

        val json = JsonObject.fromValues(values, includeNulls).toString()

        return MockPermission(
            blockchainIdentifier,
            accountId,
            senderId,
            connectedAt,
        ) to json
    }
}