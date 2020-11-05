package it.airgap.beaconsdk.internal.transport.p2p.matrix.network

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.internal.network.HttpClient
import it.airgap.beaconsdk.internal.network.data.ApplicationJson
import it.airgap.beaconsdk.internal.network.provider.HttpClientProvider
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.login.MatrixLoginRequest
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.login.MatrixLoginResponse
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

internal class MatrixUserServiceTest {

    @MockK
    private lateinit var httpClientProvider: HttpClientProvider
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

        coEvery { httpClientProvider.post<MatrixLoginRequest, MatrixLoginResponse>(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        ) } returns expectedResponse

        runBlockingTest {
            val user = "user"
            val password = "passowrd"
            val deviceId = "deviceId"

            val request = MatrixLoginRequest.Password(
                MatrixLoginRequest.UserIdentifier.User(user),
                password,
                deviceId,
            )
            val response = matrixUserService.login(user, password, deviceId).value()

            assertEquals(expectedResponse, response)
            coVerify { httpClientProvider.post(
                "/login",
                listOf(ApplicationJson()),
                emptyList(),
                request,
                MatrixLoginRequest::class,
                MatrixLoginResponse::class,
                null,
            ) }

            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `returns error if login failed`() {
        val error = IOException()
        coEvery { httpClientProvider.post<MatrixLoginRequest, MatrixLoginResponse>(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        ) } throws error

        runBlockingTest {
            val exception = matrixUserService.login("user", "password", "deviceId").errorOrNull()

            assertEquals(error, exception)
        }
    }
}