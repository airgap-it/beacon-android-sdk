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
import kotlinx.serialization.json.encodeToJsonElement
import mockDependencyRegistry
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class TezosPermissionTest {

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
        listOf(
            expectedWithJson(),
            expectedWithJson(blockchainIdentifier = null),
            expectedWithJson(includeNulls = true),
        ).map {
            json.decodeFromString<TezosPermission>(it.second) to it.first
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
            "network" to json.encodeToJsonElement(network),
            "appMetadata" to json.encodeToJsonElement(appMetadata),
            "scopes" to json.encodeToJsonElement(scopes),
        )

        val jsonObject = JsonObject.fromValues(values, includeNulls).toString()

        return TezosPermission(
            accountId,
            senderId,
            connectedAt,
            address,
            publicKey,
            network,
            appMetadata,
            scopes,
        ) to jsonObject
    }
}