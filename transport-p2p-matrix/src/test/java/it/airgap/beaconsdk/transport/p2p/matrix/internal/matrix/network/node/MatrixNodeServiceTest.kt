package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.node

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.internal.network.HttpClient
import it.airgap.beaconsdk.core.network.provider.HttpClientProvider
import it.airgap.beaconsdk.core.network.provider.HttpProvider
import it.airgap.beaconsdk.transport.p2p.matrix.internal.BeaconP2pMatrixConfiguration
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.MatrixError
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.node.MatrixVersionsResponse
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class MatrixNodeServiceTest {

    @MockK
    private lateinit var httpClientProvider: HttpClientProvider

    private lateinit var httpClient: HttpClient
    private lateinit var matrixNodeService: MatrixNodeService
    private lateinit var json: Json

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        json = Json(from = Json.Default) {
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

        runBlockingTest {
            assertTrue(matrixNodeService.isUp(upNode), "Expected upNode to be recognized as up.")
            assertFalse(matrixNodeService.isUp(downNode), "Expected downNode to be recognized as down.")
        }
    }
}