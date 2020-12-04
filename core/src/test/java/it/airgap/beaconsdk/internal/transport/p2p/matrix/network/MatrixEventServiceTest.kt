package it.airgap.beaconsdk.internal.transport.p2p.matrix.network

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.internal.network.HttpClient
import it.airgap.beaconsdk.internal.network.data.ApplicationJson
import it.airgap.beaconsdk.internal.network.data.BearerHeader
import it.airgap.beaconsdk.internal.network.data.HttpParameter
import it.airgap.beaconsdk.internal.network.provider.HttpClientProvider
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.event.MatrixEventResponse
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncResponse
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncStateEvent.Message
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

internal class MatrixEventServiceTest {

    @MockK
    private lateinit var httpClientProvider: HttpClientProvider
    private lateinit var httpClient: HttpClient

    private lateinit var matrixEventService: MatrixEventService

    private lateinit var testDeferred: CompletableDeferred<Unit>

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        httpClient = HttpClient(httpClientProvider)
        matrixEventService = MatrixEventService(httpClient)

        testDeferred = CompletableDeferred()
    }

    @Test
    fun `gets all events from server`() {
        val expectedResponse = MatrixSyncResponse()
        coEvery { httpClientProvider.get<MatrixSyncResponse>(any(), any(), any(), any(), any()) } returns expectedResponse

        runBlocking {
            val accessToken = "token"

            val response = matrixEventService.sync(accessToken).get()

            assertEquals(expectedResponse, response)
            coVerify { httpClientProvider.get(
                "/sync",
                listOf(BearerHeader(accessToken), ApplicationJson()),
                emptyList(),
                MatrixSyncResponse::class,
                null,
            ) }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `returns error if sync failed`() {
        val error = IOException()
        coEvery { httpClientProvider.get<MatrixSyncResponse>(any(), any(), any(), any(), any()) } throws error

        runBlocking {
            val exception = matrixEventService.sync("token").errorOrNull()

            assertEquals(error, exception)
        }
    }

    @Test
    fun `gets latest events from server based on specified token`() {
        val expectedResponse = MatrixSyncResponse()
        coEvery { httpClientProvider.get<MatrixSyncResponse>(allAny(), allAny(), allAny(), allAny(), allAny()) } returns expectedResponse

        runBlocking {
            val accessToken = "token"
            val since = "since"
            val timeout = 2L

            val response = matrixEventService.sync(accessToken, since, timeout).get()

            assertEquals(expectedResponse, response)
            coVerify { httpClientProvider.get(
                "/sync",
                listOf(BearerHeader(accessToken), ApplicationJson()),
                listOf(HttpParameter("since", since), HttpParameter("timeout", timeout.toString())),
                MatrixSyncResponse::class,
                3L,
            ) }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `has only one sync request pending at a time`() {
        val expectedResponse = MatrixSyncResponse()
        coEvery { httpClientProvider.get<MatrixSyncResponse>(any(), any(), any(), any(), any()) } coAnswers {
            testDeferred.await()
            expectedResponse
        }

        runBlocking {
            val accessToken = "token"

            val sync1 = launch { matrixEventService.sync(accessToken).get() }
            val sync2 = launch { matrixEventService.sync(accessToken).get() }

            testDeferred.complete(Unit)

            sync1.join()
            sync2.join()

            coVerify(exactly = 1) { httpClientProvider.get(
                "/sync",
                listOf(BearerHeader(accessToken), ApplicationJson()),
                emptyList(),
                MatrixSyncResponse::class,
                null,
            ) }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `sends text message and returns result`() {
        val expectedResponse = MatrixEventResponse("eventId")
        coEvery { httpClientProvider.put<Any, MatrixEventResponse>(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        ) } returns expectedResponse

        runBlockingTest {
            val accessToken = "accessToken"
            val roomId = "roomId"
            val txnId = "txnId"
            val message = "message"

            val response = matrixEventService.sendTextMessage(
                accessToken,
                roomId,
                txnId,
                message,
            ).get()

            assertEquals(expectedResponse, response)
            coVerify { httpClientProvider.put(
                "/rooms/$roomId/send/m.room.message/$txnId",
                listOf(BearerHeader(accessToken), ApplicationJson()),
                emptyList(),
                Message.Content(Message.TYPE_TEXT, message),
                Message.Content::class,
                MatrixEventResponse::class,
                any(),
            ) }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `returns error if sending text message failed`() {
        val error = IOException()
        coEvery { httpClientProvider.put<Any, MatrixEventResponse>(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        ) } throws error

        runBlockingTest {
            val exception = matrixEventService.sendTextMessage(
                "token",
                "roomId",
                "txnId",
                "message",
            ).errorOrNull()

            assertEquals(error, exception)
        }
    }
}