package it.airgap.beaconsdk.transport.p2p.matrix.internal.data

import io.mockk.MockKAnnotations
import it.airgap.beaconsdk.core.internal.compat.CoreCompat
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.serializer.coreJson
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.transport.data.P2pPairingResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import mockDependencyRegistry
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class P2pPairingResponseTest {

    private lateinit var dependencyRegistry: DependencyRegistry
    private lateinit var json: Json

    private val beaconScope: BeaconScope = BeaconScope.Global

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        dependencyRegistry = mockDependencyRegistry()
        json = coreJson(dependencyRegistry.blockchainRegistry, CoreCompat(beaconScope))
    }

    @Test
    fun `is deserialized from JSON`() {
        listOf(expectedWithJson())
            .map { json.decodeFromString<P2pPairingResponse>(it.second) to it.first }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `serializes to JSON`() {
        listOf(expectedWithJson())
            .map {
                json.decodeFromString(JsonObject.serializer(), json.encodeToString(it.first)) to
                        json.decodeFromString(JsonObject.serializer(), it.second)
            }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    private fun expectedWithJson(
        id: String = "id",
        name: String = "name",
        version: String = "version",
        publicKey: String = "publicKey",
        relayServer: String = "relayServer",
    ): Pair<P2pPairingResponse, String> =
        P2pPairingResponse(id, name, version, publicKey, relayServer) to """
            {
                "id": "$id",
                "type": "${P2pPairingResponse.TYPE}",
                "name": "$name",
                "version": "$version",
                "publicKey": "$publicKey",
                "relayServer": "$relayServer"
            }
        """.trimIndent()
}