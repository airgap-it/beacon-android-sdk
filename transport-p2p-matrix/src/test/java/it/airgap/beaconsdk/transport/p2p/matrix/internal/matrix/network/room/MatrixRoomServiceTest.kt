package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.room

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.internal.network.HttpClient
import it.airgap.beaconsdk.core.internal.network.data.ApplicationJson
import it.airgap.beaconsdk.core.internal.network.data.BearerHeader
import it.airgap.beaconsdk.core.network.provider.HttpProvider
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.MatrixError
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.room.*
import kotlinx.coroutines.test.runBlockingTest
import nodeApiUrl
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

internal class MatrixRoomServiceTest {

    @MockK
    private lateinit var httpClientProvider: HttpProvider
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

        coEvery { httpClientProvider.post<MatrixCreateRoomRequest, MatrixCreateRoomResponse, MatrixError>(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
        ) } returns expectedResponse

        runBlockingTest {
            val node = "node"
            val accessToken = "token"
            val request = MatrixCreateRoomRequest()

            val response = matrixRoomService.createRoom(node, accessToken, request).getOrThrow()

            assertEquals(expectedResponse, response)
            coVerify { httpClientProvider.post(
                nodeApiUrl(node),
                "/createRoom",
                listOf(BearerHeader(accessToken), ApplicationJson()),
                emptyList(),
                request,
                MatrixCreateRoomRequest::class,
                MatrixCreateRoomResponse::class,
                MatrixError::class,
                null,
            ) }

            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `returns error if creating room failed`() {
        val error = IOException()
        coEvery { httpClientProvider.post<MatrixCreateRoomRequest, MatrixCreateRoomResponse, MatrixError>(
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
            val exception = matrixRoomService.createRoom("node", "token").exceptionOrNull()

            assertEquals(error, exception)
        }
    }

    @Test
    fun `invites to room and returns response`() {
        val expectedResponse = MatrixInviteRoomResponse()

        coEvery { httpClientProvider.post<MatrixInviteRoomRequest, MatrixInviteRoomResponse, MatrixError>(
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
            val accessToken = "token"
            val user = "user"
            val roomId = "roomId"

            val request = MatrixInviteRoomRequest(user)
            val response = matrixRoomService.inviteToRoom(node, accessToken, user, roomId).getOrThrow()

            assertEquals(expectedResponse, response)
            coVerify { httpClientProvider.post(
                nodeApiUrl(node),
                "/rooms/$roomId/invite",
                listOf(BearerHeader(accessToken), ApplicationJson()),
                emptyList(),
                request,
                MatrixInviteRoomRequest::class,
                MatrixInviteRoomResponse::class,
                MatrixError::class,
                null,
            ) }

            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `returns error if inviting to room failed`() {
        val error = IOException()
        coEvery { httpClientProvider.post<MatrixInviteRoomRequest, MatrixInviteRoomResponse, MatrixError>(
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
            val exception = matrixRoomService.inviteToRoom("node", "token", "user", "roomId").exceptionOrNull()

            assertEquals(error, exception)
        }
    }

    @Test
    fun `joins room and returns response`() {
        val expectedResponse = MatrixJoinRoomResponse("userId")

        coEvery { httpClientProvider.post<MatrixJoinRoomRequest, MatrixJoinRoomResponse, MatrixError>(
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
            val accessToken = "token"
            val roomId = "roomId"

            val response = matrixRoomService.joinRoom(node, accessToken, roomId).getOrThrow()

            assertEquals(expectedResponse, response)
            coVerify { httpClientProvider.post(
                nodeApiUrl(node),
                "/rooms/$roomId/join",
                listOf(BearerHeader(accessToken), ApplicationJson()),
                emptyList(),
                any(),
                MatrixJoinRoomRequest::class,
                MatrixJoinRoomResponse::class,
                MatrixError::class,
                null,
            ) }

            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `returns error if joining room failed`() {
        val error = IOException()
        coEvery { httpClientProvider.post<MatrixJoinRoomRequest, MatrixJoinRoomResponse, MatrixError>(
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
            val exception = matrixRoomService.joinRoom("node", "token", "roomId").exceptionOrNull()

            assertEquals(error, exception)
        }
    }
}