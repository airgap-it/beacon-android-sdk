package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.node

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.internal.compat.CoreCompat
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.network.HttpClient
import it.airgap.beaconsdk.core.internal.serializer.coreJson
import it.airgap.beaconsdk.core.network.provider.HttpClientProvider
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.transport.p2p.matrix.internal.BeaconP2pMatrixConfiguration
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.node.MatrixVersionsResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mockDependencyRegistry
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class MatrixNodeServiceTest {

    @MockK
    private lateinit var httpClientProvider: HttpClientProvider

    private lateinit var dependencyRegistry: DependencyRegistry
    private lateinit var httpClient: HttpClient
    private lateinit var matrixNodeService: MatrixNodeService
    private lateinit var json: Json

    private val beaconScope: BeaconScope = BeaconScope.Global

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        dependencyRegistry = mockDependencyRegistry()

        json = Json(from = coreJson(dependencyRegistry.blockchainRegistry, CoreCompat(beaconScope))) {
            prettyPrint = true
        }

        httpClient = HttpClient(httpClientProvider, json)
        matrixNodeService = MatrixNodeService(httpClient)
    }

    @Test
    fun `checks if node is up`() {
        val upNode = "upNode"
        val downNode = "downNode"

        coEvery { httpClientProvider.get(
            "https://$upNode/${BeaconP2pMatrixConfiguration.MATRIX_API_BASE.trimStart('/')}",
            any(),
            any(),
            any(),
            any(),
        ) } returns json.encodeToString(MatrixVersionsResponse(listOf()))

        coEvery { httpClientProvider.get(
            "https://$downNode/${BeaconP2pMatrixConfiguration.MATRIX_API_BASE.trimStart('/')}",
            any(),
            any(),
            any(),
            any(),
        ) } throws IOException()

        runTest {
            assertTrue(matrixNodeService.isUp(upNode), "Expected upNode to be recognized as up.")
            assertFalse(matrixNodeService.isUp(downNode), "Expected downNode to be recognized as down.")
        }
    }
}