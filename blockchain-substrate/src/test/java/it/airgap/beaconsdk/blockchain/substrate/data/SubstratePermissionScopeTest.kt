package it.airgap.beaconsdk.blockchain.substrate.data

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
import kotlinx.serialization.json.JsonArray
import mockDependencyRegistry
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class SubstratePermissionScopeTest {

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
    fun `is deserialized from string`() {
        expectedWithJsonValues()
            .map { json.decodeFromString<SubstratePermission.Scope>(it.second) to it.first }
            .forEach { assertEquals(it.second, it.first) }
    }

    @Test
    fun `serializes to string`() {
        expectedWithJsonValues()
            .map { json.encodeToString(it.first) to it.second }
            .forEach { assertEquals(it.second, it.first) }
    }

    @Test
    fun `is deserialized from list of strings`() {
        expectedWithJsonArray()
            .map { json.decodeFromString<List<SubstratePermission.Scope>>(it.second) to it.first }
            .forEach { assertEquals(it.second, it.first) }
    }

    @Test
    fun `serializes to list of string`() {
        expectedWithJsonArray()
            .map { json.decodeFromString(JsonArray.serializer(), json.encodeToString(it.first)) to
                    json.decodeFromString(JsonArray.serializer(), it.second) }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    private fun expectedWithJsonValues(): List<Pair<SubstratePermission.Scope, String>> = listOf(
        SubstratePermission.Scope.Transfer to "\"transfer\"",
        SubstratePermission.Scope.SignPayloadJson to "\"sign_payload_json\"",
        SubstratePermission.Scope.SignPayloadRaw to "\"sign_payload_raw\"",
    )

    private fun expectedWithJsonArray(): List<Pair<List<SubstratePermission.Scope>, String>> = listOf(
        listOf(SubstratePermission.Scope.Transfer, SubstratePermission.Scope.SignPayloadJson, SubstratePermission.Scope.SignPayloadRaw) to
                """
                    [
                        "transfer",
                        "sign_payload_json",
                        "sign_payload_raw"
                    ]
                """.trimIndent(),
    )
}