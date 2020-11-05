package it.airgap.beaconsdk.internal.transport.p2p.matrix.network

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.internal.network.HttpClient
import it.airgap.beaconsdk.internal.network.data.ApplicationJson
import it.airgap.beaconsdk.internal.network.data.BearerHeader
import it.airgap.beaconsdk.internal.network.provider.HttpClientProvider
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.room.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

internal class MatrixRoomServiceTest {

    @MockK
    private lateinit var httpClientProvider: HttpClientProvider
    private lateinit var httpClient: HttpClient

    private lateinit var matrixRoomService: MatrixRoomService

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        httpClient = HttpClient(httpClientProvider)
        matrixRoomService = MatrixRoomService(httpClient)
    }

    @Test
    fun `creates room and returns response`() {
        val expectedResponse = MatrixCreateRoomResponse("roomId")

        coEvery { httpClientProvider.post<MatrixCreateRoomRequest, MatrixCreateRoomResponse>(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        ) } returns expectedResponse

        runBlockingTest {
            val accessToken = "token"
            val request = MatrixCreateRoomRequest()

            val response = matrixRoomService.createRoom(accessToken, request).value()

            assertEquals(expectedResponse, response)
            coVerify { httpClientProvider.post(
                "/createRoom",
                listOf(BearerHeader(accessToken), ApplicationJson()),
                emptyList(),
                request,
                MatrixCreateRoomRequest::class,
                MatrixCreateRoomResponse::class,
                null,
            ) }

            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `returns error if creating room failed`() {
        val error = IOException()
        coEvery { httpClientProvider.post<MatrixCreateRoomRequest, MatrixCreateRoomResponse>(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        ) } throws error

        runBlockingTest {
            val exception = matrixRoomService.createRoom("token").errorOrNull()

            assertEquals(error, exception)
        }
    }

    @Test
    fun `invites to room and returns response`() {
        val expectedResponse = MatrixInviteRoomResponse()

        coEvery { httpClientProvider.post<MatrixInviteRoomRequest, MatrixInviteRoomResponse>(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        ) } returns expectedResponse

        runBlockingTest {
            val accessToken = "token"
            val user = "user"
            val roomId = "roomId"

            val request = MatrixInviteRoomRequest(user)
            val response = matrixRoomService.inviteToRoom(accessToken, user, roomId).value()

            assertEquals(expectedResponse, response)
            coVerify { httpClientProvider.post(
                "/rooms/$roomId/invite",
                listOf(BearerHeader(accessToken), ApplicationJson()),
                emptyList(),
                request,
                MatrixInviteRoomRequest::class,
                MatrixInviteRoomResponse::class,
                null,
            ) }

            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `returns error if inviting to room failed`() {
        val error = IOException()
        coEvery { httpClientProvider.post<MatrixInviteRoomRequest, MatrixInviteRoomResponse>(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        ) } throws error

        runBlockingTest {
            val exception = matrixRoomService.inviteToRoom("token", "user", "roomId").errorOrNull()

            assertEquals(error, exception)
        }
    }

    @Test
    fun `joins room and returns response`() {
        val expectedResponse = MatrixJoinRoomResponse("userId")

        coEvery { httpClientProvider.post<MatrixJoinRoomRequest, MatrixJoinRoomResponse>(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        ) } returns expectedResponse

        runBlockingTest {
            val accessToken = "token"
            val roomId = "roomId"

            val response = matrixRoomService.joinRoom(accessToken, roomId).value()

            assertEquals(expectedResponse, response)
            coVerify { httpClientProvider.post(
                "/rooms/$roomId/join",
                listOf(BearerHeader(accessToken), ApplicationJson()),
                emptyList(),
                any(),
                MatrixJoinRoomRequest::class,
                MatrixJoinRoomResponse::class,
                null,
            ) }

            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `returns error if joining room failed`() {
        val error = IOException()
        coEvery { httpClientProvider.post<MatrixJoinRoomRequest, MatrixJoinRoomResponse>(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        ) } throws error

        runBlockingTest {
            val exception = matrixRoomService.joinRoom("token", "roomId").errorOrNull()

            assertEquals(error, exception)
        }
    }
}