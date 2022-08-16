package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.event

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.internal.network.HttpClient
import it.airgap.beaconsdk.core.internal.network.data.ApplicationJson
import it.airgap.beaconsdk.core.internal.network.data.BearerHeader
import it.airgap.beaconsdk.core.internal.utils.encodeToString
import it.airgap.beaconsdk.core.network.data.HttpParameter
import it.airgap.beaconsdk.core.network.provider.HttpClientProvider
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.MatrixError
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.event.MatrixEventResponse
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncResponse
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncStateEvent.Message
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nodeApiUrl
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

internal class MatrixEventServiceTest {

    @MockK
    private lateinit var httpClientProvider: HttpClientProvider

    private lateinit var httpClient: HttpClient
    private lateinit var matrixEventService: MatrixEventService
    private lateinit var json: Json

    private lateinit var testDeferred: CompletableDeferred<Unit>

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        json = Json(from = Json.Default) {
            prettyPrint = true
        }

        httpClient = HttpClient(httpClientProvider, json)
        matrixEventService = MatrixEventService(httpClient)

        testDeferred = CompletableDeferred()
    }

    @Test
    fun `gets all events from server`() {
        val expectedResponse = MatrixSyncResponse()
        coEvery { httpClientProvider.get(any(), any(), any(), any(), any()) } returns json.encodeToString(expectedResponse)

        runBlocking {
            val node = "node"
            val accessToken = "token"

            val response = matrixEventService.sync(node, accessToken).getOrThrow()

            assertEquals(expectedResponse, response)
            coVerify { httpClientProvider.get(
                nodeApiUrl(node),
                "/sync",
                listOf(BearerHeader(accessToken), ApplicationJson()),
                emptyList(),
                null,
            ) }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `returns error if sync failed`() {
        val error = IOException()
        coEvery { httpClientProvider.get(any(), any(), any(), any(), any()) } throws error

        runBlocking {
            val exception = matrixEventService.sync("node", "token").exceptionOrNull()

            assertEquals(error, exception)
        }
    }

    @Test
    fun `gets latest events from server based on specified token`() {
        val expectedResponse = MatrixSyncResponse()
        coEvery { httpClientProvider.get(any(), any(), any(), any(), any()) } returns json.encodeToString(expectedResponse)

        runBlocking {
            val node = "node"
            val accessToken = "token"
            val since = "since"
            val timeout = 2L

            val response = matrixEventService.sync(node, accessToken, since, timeout).getOrThrow()

            assertEquals(expectedResponse, response)
            coVerify { httpClientProvider.get(
                nodeApiUrl(node),
                "/sync",
                listOf(BearerHeader(accessToken), ApplicationJson()),
                listOf(HttpParameter("since", since), HttpParameter("timeout", timeout.toString())),
                3L,
            ) }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `has only one sync request pending at a time`() {
        val expectedResponse = MatrixSyncResponse()
        coEvery { httpClientProvider.get(any(), any(), any(), any(), any()) } coAnswers {
            testDeferred.await()
            json.encodeToString(expectedResponse)
        }

        runBlocking {
            val node = "node"
            val accessToken = "token"

            val sync1 = launch { matrixEventService.sync(node, accessToken).getOrThrow() }
            val sync2 = launch { matrixEventService.sync(node, accessToken).getOrThrow() }

            testDeferred.complete(Unit)

            sync1.join()
            sync2.join()

            coVerify(exactly = 1) { httpClientProvider.get(
                nodeApiUrl(node),
                "/sync",
                listOf(BearerHeader(accessToken), ApplicationJson()),
                emptyList(),
                null,
            ) }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `sends text message and returns result`() {
        val expectedResponse = MatrixEventResponse("eventId")
        coEvery { httpClientProvider.put(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        ) } returns json.encodeToString(expectedResponse)

        runBlockingTest {
            val node = "node"
            val accessToken = "accessToken"
            val roomId = "roomId"
            val txnId = "txnId"
            val message = "message"

            val response = matrixEventService.sendTextMessage(
                node,
                accessToken,
                roomId,
                txnId,
                message,
            ).getOrThrow()

            assertEquals(expectedResponse, response)
            coVerify { httpClientProvider.put(
                nodeApiUrl(node),
                "/rooms/$roomId/send/m.room.message/$txnId",
                listOf(BearerHeader(accessToken), ApplicationJson()),
                emptyList(),
                json.encodeToString(Message.Content(Message.Content.TYPE_TEXT, message)),
                any(),
            ) }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `returns error if sending text message failed`() {
        val error = IOException()
        coEvery { httpClientProvider.put(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        ) } throws error

        runBlockingTest {
            val exception = matrixEventService.sendTextMessage(
                "node",
                "token",
                "roomId",
                "txnId",
                "message",
            ).exceptionOrNull()

            assertEquals(error, exception)
        }
    }
}