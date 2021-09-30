package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.user

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.internal.network.HttpClient
import it.airgap.beaconsdk.core.internal.network.data.ApplicationJson
import it.airgap.beaconsdk.core.network.provider.HttpProvider
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.MatrixError
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.login.MatrixLoginRequest
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.login.MatrixLoginResponse
import kotlinx.coroutines.test.runBlockingTest
import nodeApiUrl
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

internal class MatrixUserServiceTest {

    @MockK
    private lateinit var httpClientProvider: HttpProvider
    private lateinit var httpClient: HttpClient

    private lateinit var matrixUserService: MatrixUserService

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        httpClient = HttpClient(httpClientProvider)
        matrixUserService = MatrixUserService(httpClient)
    }

    @Test
    fun `logs in to Matrix server and returns response`() {
        val expectedResponse = MatrixLoginResponse("userId", "deviceId", "accessToken")

        coEvery { httpClientProvider.post<MatrixLoginRequest, MatrixLoginResponse, MatrixError>(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        ) } returns expectedResponse

        runBlockingTest {
            val node = "node"
            val user = "user"
            val password = "password"
            val deviceId = "deviceId"

            val request = MatrixLoginRequest.Password(
                MatrixLoginRequest.UserIdentifier.User(user),
                password,
                deviceId,
            )
            val response = matrixUserService.login(node, user, password, deviceId).getOrThrow()

            assertEquals(expectedResponse, response)
            coVerify { httpClientProvider.post(
                nodeApiUrl(node),
                "/login",
                listOf(ApplicationJson()),
                emptyList(),
                request,
                MatrixLoginRequest::class,
                MatrixLoginResponse::class,
                MatrixError::class,
                null,
            ) }

            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `returns error if login failed`() {
        val error = IOException()
        coEvery { httpClientProvider.post<MatrixLoginRequest, MatrixLoginResponse, MatrixError>(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        ) } throws error

        runBlockingTest {
            val exception = matrixUserService.login("node", "user", "password", "deviceId").exceptionOrNull()

            assertEquals(error, exception)
        }
    }
}