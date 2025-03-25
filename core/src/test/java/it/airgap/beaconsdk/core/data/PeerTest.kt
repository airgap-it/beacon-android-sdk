package it.airgap.beaconsdk.core.data

import fromValues
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

internal class PeerTest {

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
        (expectedWithJson() + expectedWithJson(includeNulls = true))
            .map { json.decodeFromString<Peer>(it.second) to it.first }
            .forEach { assertEquals(it.second, it.first) }
    }

    @Test
    fun `serializes to JSON`() {
        expectedWithJson()
            .map {
                json.decodeFromString(JsonObject.serializer(), json.encodeToString(it.first)) to
                        json.decodeFromString(JsonObject.serializer(), it.second)
            }.forEach {
                assertEquals(it.second, it.first)
            }
    }

    private fun expectedWithJson(
        name: String = "name",
        publicKey: String = "publicKey",
        version: String = "version",
        includeNulls: Boolean = false,
    ): List<Pair<Peer, String>> = listOf(
        expectedP2pWithJson(name, publicKey, version, includeNulls = includeNulls),
        expectedP2pWithJson(name, publicKey, version, icon = "icon", includeNulls = includeNulls),
        expectedP2pWithJson(name, publicKey, version, appUrl = "appUrl", includeNulls = includeNulls),
        expectedP2pWithJson(name, publicKey, version, icon = "icon", appUrl = "appUrl", includeNulls = includeNulls),
    )

    private fun expectedP2pWithJson(
        name: String = "name",
        publicKey: String = "publicKey",
        version: String = "version",
        relayServer: String = "relayServer",
        icon: String? = null,
        appUrl: String? = null,
        includeNulls: Boolean = false,
    ): Pair<P2pPeer, String> =
        P2pPeer(name, publicKey, relayServer, version, icon, appUrl) to
                p2pJson(name, publicKey, relayServer, version, icon, appUrl, includeNulls)

    private fun p2pJson(
        name: String = "name",
        publicKey: String = "publicKey",
        relayServer: String = "relayServer",
        version: String = "version",
        icon: String? = null,
        appUrl: String? = null,
        includeNulls: Boolean = false,
    ): String {
        val values = mapOf(
            "type" to "p2p",
            "name" to name,
            "publicKey" to publicKey,
            "relayServer" to relayServer,
            "version" to version,
            "icon" to icon,
            "appUrl" to appUrl,
        )

        return JsonObject.fromValues(values, includeNulls).toString()
    }
}