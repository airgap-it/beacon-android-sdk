package it.airgap.beaconsdk.core.data

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.compat.CoreCompat
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.serializer.coreJson
import it.airgap.beaconsdk.core.scope.BeaconScope
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import mockBeaconSdk
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class OriginTest {

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

        json = coreJson(dependencyRegistry.blockchainRegistry, CoreCompat(beaconScope))
    }

    @Test
    fun `is deserialized from JSON`() {
        expectedWithJsonStrings()
            .map { json.decodeFromString<Origin>(it.second) to it.first }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `serializes to JSON`() {
        expectedWithJsonStrings()
            .map {
                json.decodeFromString(JsonObject.serializer(), json.encodeToString(it.first)) to
                        json.decodeFromString(JsonObject.serializer(), it.second)
            }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `creates origin from peer`() {
        expectedWithPeers()
            .map { it.first to Origin.forPeer(it.second) }
            .forEach { assertEquals(it.first, it.second) }
    }

    private fun expectedWithJsonStrings(id: String = "id"): List<Pair<Origin, String>> = listOf(
        Origin.Website(id) to json("website", id),
        Origin.Extension(id) to json("extension", id),
        Origin.P2P(id) to json("p2p", id),
    )

    private fun expectedWithPeers(publicKey: String = "publicKey"): List<Pair<Origin, Peer>> = listOf(
        Origin.P2P(publicKey) to P2pPeer(name = "name", publicKey = publicKey, relayServer = "relayServer"),
    )

    private fun json(type: String, id: String): String =
        """
            {
                "type": "$type",
                "id": "$id"
            }
        """.trimIndent()
}