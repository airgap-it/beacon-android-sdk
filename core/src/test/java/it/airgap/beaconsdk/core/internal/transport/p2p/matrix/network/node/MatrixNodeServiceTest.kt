package it.airgap.beaconsdk.core.internal.transport.p2p.matrix.network.node

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.network.HttpClient
import it.airgap.beaconsdk.core.network.provider.HttpProvider
import it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data.api.MatrixError
import it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data.api.node.MatrixVersionsResponse
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class MatrixNodeServiceTest {

    @MockK
    private lateinit var httpClientProvider: HttpProvider
    private lateinit var httpClient: HttpClient

    private lateinit var matrixNodeService: MatrixNodeService

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        httpClient = HttpClient(httpClientProvider)
        matrixNodeService = MatrixNodeService(httpClient)
    }

    @Test
    fun `checks if node is up`() {
        val upNode = "upNode"
        val downNode = "downNode"

        coEvery { httpClientProvider.get<MatrixVersionsResponse, MatrixError>(
            "https://$upNode/${BeaconConfiguration.MATRIX_CLIENT_API_BASE.trimStart('/')}",
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        ) } returns MatrixVersionsResponse(listOf())

        coEvery { httpClientProvider.get<MatrixVersionsResponse, MatrixError>(
            "https://$downNode/${BeaconConfiguration.MATRIX_CLIENT_API_BASE.trimStart('/')}",
            any(),
            any(),
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