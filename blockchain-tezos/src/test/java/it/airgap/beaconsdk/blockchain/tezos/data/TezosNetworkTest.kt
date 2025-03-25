package it.airgap.beaconsdk.blockchain.tezos.data

import fromValues
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.blockchain.tezos.Tezos
import it.airgap.beaconsdk.blockchain.tezos.internal.creator.*
import it.airgap.beaconsdk.blockchain.tezos.internal.serializer.*
import it.airgap.beaconsdk.blockchain.tezos.internal.wallet.TezosWallet
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

internal class TezosNetworkTest {
    @MockK
    private lateinit var wallet: TezosWallet

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
        val tezos = Tezos(
            wallet,
            TezosCreator(
                DataTezosCreator(storageManager, identifierCreator),
                V1BeaconMessageTezosCreator(),
                V2BeaconMessageTezosCreator(),
                V3BeaconMessageTezosCreator(),
            ),
            TezosSerializer(
                DataTezosSerializer(),
                V1BeaconMessageTezosSerializer(),
                V2BeaconMessageTezosSerializer(),
                V3BeaconMessageTezosSerializer(),
            ),
        )

        dependencyRegistry = mockDependencyRegistry(tezos)
        json = coreJson(dependencyRegistry.blockchainRegistry, CoreCompat(beaconScope))
    }

    @Test
    fun `is deserialized from JSON`() {
        (
                expectedWithJsonStrings() +
                expectedWithJsonStrings(includeNulls = true) +
                expectedWithJsonStrings(name = "name") +
                expectedWithJsonStrings(rpcUrl = "rpcUrl") +
                expectedWithJsonStrings(name = "name", rpcUrl = "rpcUrl")
        ).map {
            json.decodeFromString<TezosNetwork>(it.second) to it.first
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
            json.decodeFromString(JsonObject.serializer(), json.encodeToString(it.first)) to
                    json.decodeFromString(JsonObject.serializer(), it.second)
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    private fun expectedWithJsonStrings(
        name: String? = null,
        rpcUrl: String? = null,
        includeNulls: Boolean = false,
    ): List<Pair<TezosNetwork, String>> = listOf(
        TezosNetwork.Mainnet(name, rpcUrl) to json("mainnet", name, rpcUrl, includeNulls),
        TezosNetwork.Ghostnet(name, rpcUrl) to json("ghostnet", name, rpcUrl, includeNulls),
        TezosNetwork.Mondaynet(name, rpcUrl) to json("mondaynet", name, rpcUrl, includeNulls),
        TezosNetwork.Dailynet(name, rpcUrl) to json("dailynet", name, rpcUrl, includeNulls),
        TezosNetwork.Custom(name, rpcUrl) to json("custom", name, rpcUrl, includeNulls),
    )

    private fun json(
        identifier: String,
        name: String? = null,
        rpcUrl: String? = null,
        includeNulls: Boolean = false,
    ): String {
        val values = mapOf(
            "type" to identifier,
            "name" to name,
            "rpcUrl" to rpcUrl,
        )

        return JsonObject.fromValues(values, includeNulls).toString()
    }
}