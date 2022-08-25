package it.airgap.beaconsdk.blockchain.substrate.data

import fromValues
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.blockchain.substrate.Substrate
import it.airgap.beaconsdk.blockchain.substrate.internal.creator.*
import it.airgap.beaconsdk.blockchain.substrate.internal.serializer.*
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.compat.CoreCompat
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.serializer.coreJson
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.scope.BeaconScope
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import mockDependencyRegistry
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class SubstrateNetworkTest {

    @MockK
    private lateinit var identifierCreator: IdentifierCreator

    private lateinit var dependencyRegistry: DependencyRegistry
    private lateinit var storageManager: StorageManager

    private lateinit var json: Json

    private val beaconScope: BeaconScope = BeaconScope.Global

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        storageManager = StorageManager(beaconScope, MockStorage(), MockSecureStorage(), identifierCreator, BeaconConfiguration(ignoreUnsupportedBlockchains = false))
        val substrate = Substrate(
            SubstrateCreator(
                DataSubstrateCreator(storageManager, identifierCreator),
                V1BeaconMessageSubstrateCreator(),
                V2BeaconMessageSubstrateCreator(),
                V3BeaconMessageSubstrateCreator(),
            ),
            SubstrateSerializer(
                DataSubstrateSerializer(),
                V1BeaconMessageSubstrateSerializer(),
                V2BeaconMessageSubstrateSerializer(),
                V3BeaconMessageSubstrateSerializer(),
            ),
        )

        dependencyRegistry = mockDependencyRegistry(substrate)
        every { dependencyRegistry.storageManager } returns storageManager
        every { dependencyRegistry.identifierCreator } returns identifierCreator

        json = coreJson(dependencyRegistry.blockchainRegistry, CoreCompat(beaconScope))
    }

    @Test
    fun `is deserialized from JSON`() {
        listOf(
            expectedWithJsonStrings(),
            expectedWithJsonStrings(includeNulls = true),
            expectedWithJsonStrings(name = "name"),
            expectedWithJsonStrings(rpcUrl = "rpcUrl"),
            expectedWithJsonStrings(name = "name", rpcUrl = "rpcUrl"),
        ).map {
            json.decodeFromString<SubstrateNetwork>(it.second) to it.first
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    @Test
    fun `serializes to JSON`() {
        listOf(
            expectedWithJsonStrings(),
            expectedWithJsonStrings(name = "name"),
            expectedWithJsonStrings(rpcUrl = "rpcUrl"),
            expectedWithJsonStrings(name = "name", rpcUrl = "rpcUrl"),
        ).map {
            json.decodeFromString(JsonObject.serializer(), json.encodeToString(it.first)) to
                    json.decodeFromString(JsonObject.serializer(), it.second)
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    private fun expectedWithJsonStrings(
        genesisHash: String = "genesisHash",
        name: String? = null,
        rpcUrl: String? = null,
        includeNulls: Boolean = false,
    ): Pair<SubstrateNetwork, String> {
        val values = mapOf(
            "genesisHash" to genesisHash,
            "name" to name,
            "rpcUrl" to rpcUrl,
        )

        val jsonObject = JsonObject.fromValues(values, includeNulls).toString()

        return SubstrateNetwork(genesisHash, name, rpcUrl) to jsonObject
    }
}