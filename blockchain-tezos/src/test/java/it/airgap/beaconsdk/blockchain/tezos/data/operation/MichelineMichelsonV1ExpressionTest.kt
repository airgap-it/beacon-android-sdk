package it.airgap.beaconsdk.blockchain.tezos.data.operation

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
import it.airgap.beaconsdk.core.internal.serializer.contextualJson
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.scope.BeaconScope
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import mockDependencyRegistry
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class MichelineMichelsonV1ExpressionTest {
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
        json = contextualJson(dependencyRegistry.blockchainRegistry, CoreCompat(beaconScope))
    }

    @Test
    fun `is deserialized from JSON`() {
        listOf(
            primitiveIntWithJson(),
            primitiveStringWithJson(),
            primitiveBytesWithJson(),
            primitiveApplicationWithJson(),
            primitiveApplicationWithJson(includeNulls = true),
            nodeWithJson(),
            nodeWithJson(listOf(MichelinePrimitiveInt(0))),
        ).map {
            json.decodeFromString<MichelineMichelsonV1Expression>(it.second) to it.first
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    @Test
    fun `serializes to JSON`() {
        listOf(
            primitiveIntWithJson(),
            primitiveStringWithJson(),
            primitiveBytesWithJson(),
            primitiveApplicationWithJson(),
            nodeWithJson(),
            nodeWithJson(listOf(MichelinePrimitiveInt(0))),
        ).map {
            json.decodeFromString(JsonElement.serializer(), json.encodeToString(it.first)) to
                    json.decodeFromString(JsonElement.serializer(), it.second)
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    private fun primitiveIntWithJson(int: Int = 0) : Pair<MichelinePrimitiveInt, String> =
        MichelinePrimitiveInt(int) to """
            {
                "int": "$int"
            }
        """.trimIndent()

    private fun primitiveStringWithJson(string: String = "string") : Pair<MichelinePrimitiveString, String> =
        MichelinePrimitiveString(string) to """
            {
                "string": "$string"
            }
        """.trimIndent()

    private fun primitiveBytesWithJson(bytes: String = "bytes") : Pair<MichelinePrimitiveBytes, String> =
        MichelinePrimitiveBytes(bytes) to """
                {
                    "bytes": "$bytes"
                }
            """.trimIndent()

    private fun primitiveApplicationWithJson(
        prim: String = "prim",
        args: List<MichelineMichelsonV1Expression>? = null,
        annots: List<String>? = null,
        includeNulls: Boolean = false,
    ) : Pair<MichelinePrimitiveApplication, String> {
        val values = mapOf(
            "prim" to prim,
            "args" to args?.let { json.encodeToJsonElement(it) },
            "annots" to annots?.let { json.encodeToJsonElement(it) },
        )

        val jsonObject = JsonObject.fromValues(values, includeNulls).toString()

        return MichelinePrimitiveApplication(prim, args, annots) to jsonObject
    }

    private fun nodeWithJson(expressions: List<MichelineMichelsonV1Expression> = emptyList()) : Pair<MichelineNode, String> =
        MichelineNode(expressions) to json.encodeToString(expressions)
}