package it.airgap.beaconsdk.internal.network

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.internal.network.data.ApplicationJson
import it.airgap.beaconsdk.internal.network.data.HttpParameter
import it.airgap.beaconsdk.internal.network.provider.HttpClientProvider
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class HttpClientTest {

    @MockK
    private lateinit var httpClientProvider: HttpClientProvider

    private lateinit var httpClient: HttpClient

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        httpClient = HttpClient(httpClientProvider)
    }

    @Test
    fun `makes HTTP GET call to specified endpoint`() {
        coEvery { httpClientProvider.get<Any>(any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val endpoint = "endpoint"

            httpClient.get<MockResponse>(endpoint).single()

            coVerify { httpClientProvider.get(endpoint, emptyList(), emptyList(), MockResponse::class, null) }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP GET call to specified endpoint with custom headers`() {
        coEvery { httpClientProvider.get<Any>(any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val endpoint = "endpoint"
            val headers = listOf(ApplicationJson())

            httpClient.get<MockResponse>(endpoint, headers = headers).single()

            coVerify { httpClientProvider.get(endpoint, headers, emptyList(), MockResponse::class, null) }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP GET call to specified endpoint with custom parameters`() {
        coEvery { httpClientProvider.get<Any>(any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val endpoint = "endpoint"
            val parameters = listOf(HttpParameter("key", "value"))

            httpClient.get<MockResponse>(endpoint, parameters = parameters).single()

            coVerify { httpClientProvider.get(endpoint, emptyList(), parameters, MockResponse::class, null) }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP GET call to specified endpoint with custom timeout`() {
        coEvery { httpClientProvider.get<Any>(any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val endpoint = "endpoint"
            val timeout = 1L

            httpClient.get<MockResponse>(endpoint, timeoutMillis = timeout).single()

            coVerify { httpClientProvider.get(endpoint, emptyList(), emptyList(), MockResponse::class, timeout) }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `emits success result with response on successful get`() {
        val response = MockResponse()
        coEvery { httpClientProvider.get<MockResponse>(any(), any(), any(), any(), any()) } returns response

        runBlockingTest {
            val result = httpClient.get<MockResponse>("endpoint").single()

            assertTrue(result.isSuccess, "Expected GET result to be a success")
            assertEquals(response, result.value())
        }
    }

    @Test
    fun `emits failure result with data if get failed`() {
        coEvery { httpClientProvider.get<Any>(any(), any(), any(), any(), any()) } throws IOException()

        runBlockingTest {
            val result = httpClient.get<Any>("endpoint").single()

            assertTrue(result.isFailure, "Expected GET result to be a failure")
        }
    }

    @Test
    fun `makes HTTP POST call to specified endpoint`() {
        coEvery { httpClientProvider.post<Any, Any>(any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val endpoint = "endpoint"

            httpClient.post<MockRequest, MockResponse>(endpoint).single()

            coVerify {
                httpClientProvider.post(
                    endpoint,
                    emptyList(),
                    emptyList(),
                    null,
                    MockRequest::class,
                    MockResponse::class,
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP POST call to specified endpoint with body`() {
        coEvery { httpClientProvider.post<Any, Any>(any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val endpoint = "endpoint"
            val body = MockRequest()

            httpClient.post<MockRequest, MockResponse>(endpoint, body = body).single()

            coVerify {
                httpClientProvider.post(
                    endpoint,
                    emptyList(),
                    emptyList(),
                    body,
                    MockRequest::class,
                    MockResponse::class,
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP POST call to specified endpoint with custom headers`() {
        coEvery { httpClientProvider.post<Any, Any>(any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val endpoint = "endpoint"
            val headers = listOf(ApplicationJson())

            httpClient.post<MockRequest, MockResponse>(endpoint, headers = headers).single()

            coVerify {
                httpClientProvider.post(
                    endpoint,
                    headers,
                    emptyList(),
                    null,
                    MockRequest::class,
                    MockResponse::class,
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP POST call to specified endpoint with custom parameters`() {
        coEvery { httpClientProvider.post<Any, Any>(any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val endpoint = "endpoint"
            val parameters = listOf(HttpParameter("key", "value"))

            httpClient.post<MockRequest, MockResponse>(endpoint, parameters = parameters).single()

            coVerify {
                httpClientProvider.post(
                    endpoint,
                    emptyList(),
                    parameters,
                    null,
                    MockRequest::class,
                    MockResponse::class,
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP POST call to specified endpoint with custom timeout`() {
        coEvery { httpClientProvider.post<Any, Any>(any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val endpoint = "endpoint"
            val timeout = 1L

            httpClient.post<MockRequest, MockResponse>(endpoint, timeoutMillis = timeout).single()

            coVerify {
                httpClientProvider.post(
                    endpoint,
                    emptyList(),
                    emptyList(),
                    null,
                    MockRequest::class,
                    MockResponse::class,
                    timeout,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `emits success result with response on successful post`() {
        val response = MockResponse()
        coEvery { httpClientProvider.post<Any, MockResponse>(any(), any(), any(), any(), any(), any(), any()) } returns response

        runBlockingTest {
            val result = httpClient.post<Any, MockResponse>("endpoint").single()

            assertTrue(result.isSuccess, "Expected POST result to be a success")
            assertEquals(response, result.value())
        }
    }

    @Test
    fun `emits failure result with data if post failed`() {
        coEvery { httpClientProvider.post<Any, Any>(any(), any(), any(), any(), any(), any(), any()) } throws IOException()

        runBlockingTest {
            val result = httpClient.post<Any, Any>("endpoint").single()

            assertTrue(result.isFailure, "Expected POST result to be a failure")
        }
    }

    @Test
    fun `makes HTTP PUT call to specified endpoint`() {
        coEvery { httpClientProvider.put<Any, Any>(any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val endpoint = "endpoint"

            httpClient.put<MockRequest, MockResponse>(endpoint).single()

            coVerify {
                httpClientProvider.put(
                    endpoint,
                    emptyList(),
                    emptyList(),
                    null,
                    MockRequest::class,
                    MockResponse::class,
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP PUT call to specified endpoint with body`() {
        coEvery { httpClientProvider.put<Any, Any>(any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val endpoint = "endpoint"
            val body = MockRequest()

            httpClient.put<MockRequest, MockResponse>(endpoint, body = body).single()

            coVerify {
                httpClientProvider.put(
                    endpoint,
                    emptyList(),
                    emptyList(),
                    body,
                    MockRequest::class,
                    MockResponse::class,
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP PUT call to specified endpoint with custom headers`() {
        coEvery { httpClientProvider.put<Any, Any>(any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val endpoint = "endpoint"
            val headers = listOf(ApplicationJson())

            httpClient.put<MockRequest, MockResponse>(endpoint, headers = headers).single()

            coVerify {
                httpClientProvider.put(
                    endpoint,
                    headers,
                    emptyList(),
                    null,
                    MockRequest::class,
                    MockResponse::class,
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP PUT call to specified endpoint with custom parameters`() {
        coEvery { httpClientProvider.put<Any, Any>(any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val endpoint = "endpoint"
            val parameters = listOf(HttpParameter("key", "value"))

            httpClient.put<MockRequest, MockResponse>(endpoint, parameters = parameters).single()

            coVerify {
                httpClientProvider.put(
                    endpoint,
                    emptyList(),
                    parameters,
                    null,
                    MockRequest::class,
                    MockResponse::class,
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP PUT call to specified endpoint with custom timeout`() {
        coEvery { httpClientProvider.put<Any, Any>(any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val endpoint = "endpoint"
            val timeout = 1L

            httpClient.put<MockRequest, MockResponse>(endpoint, timeoutMillis = timeout).single()

            coVerify {
                httpClientProvider.put(
                    endpoint,
                    emptyList(),
                    emptyList(),
                    null,
                    MockRequest::class,
                    MockResponse::class,
                    timeout,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `emits success result with response on successful put`() {
        val response = MockResponse()
        coEvery { httpClientProvider.put<Any, MockResponse>(any(), any(), any(), any(), any(), any(), any()) } returns response

        runBlockingTest {
            val result = httpClient.put<Any, MockResponse>("endpoint").single()

            assertTrue(result.isSuccess, "Expected PUT result to be a success")
            assertEquals(response, result.value())
        }
    }

    @Test
    fun `emits failure result with data if put failed`() {
        coEvery { httpClientProvider.put<Any, Any>(any(), any(), any(), any(), any(), any(), any()) } throws IOException()

        runBlockingTest {
            val result = httpClient.put<Any, Any>("endpoint").single()

            assertTrue(result.isFailure, "Expected PUT result to be a failure")
        }
    }

    private data class MockRequest(val body: String = "body")
    private data class MockResponse(val content: String = "content")
}