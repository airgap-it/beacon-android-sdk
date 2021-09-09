package it.airgap.beaconsdk.internal.network

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.internal.network.data.ApplicationJson
import it.airgap.beaconsdk.network.data.HttpParameter
import it.airgap.beaconsdk.network.provider.HttpProvider
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class HttpClientTest {

    @MockK
    private lateinit var httpClientProvider: HttpProvider

    private lateinit var httpClient: HttpClient

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        httpClient = HttpClient(httpClientProvider)
    }

    @Test
    fun `makes HTTP GET call to specified endpoint`() {
        coEvery { httpClientProvider.get<Any, Throwable>(any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val baseUrl = "baseUrl"
            val endpoint = "endpoint"

            httpClient.get<MockResponse, MockError>(baseUrl, endpoint).single()

            coVerify { httpClientProvider.get(baseUrl, endpoint, emptyList(), emptyList(), MockResponse::class, MockError::class, null) }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP GET call to specified endpoint with custom headers`() {
        coEvery { httpClientProvider.get<Any, Throwable>(any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val baseUrl = "baseUrl"
            val endpoint = "endpoint"
            val headers = listOf(ApplicationJson())

            httpClient.get<MockResponse, MockError>(baseUrl, endpoint, headers = headers).single()

            coVerify { httpClientProvider.get(baseUrl, endpoint, headers, emptyList(), MockResponse::class, MockError::class, null) }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP GET call to specified endpoint with custom parameters`() {
        coEvery { httpClientProvider.get<Any, Throwable>(any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val baseUrl = "baseUrl"
            val endpoint = "endpoint"
            val parameters = listOf(HttpParameter("key", "value"))

            httpClient.get<MockResponse, MockError>(baseUrl, endpoint, parameters = parameters).single()

            coVerify { httpClientProvider.get(baseUrl, endpoint, emptyList(), parameters, MockResponse::class, MockError::class, null) }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP GET call to specified endpoint with custom timeout`() {
        coEvery { httpClientProvider.get<Any, Throwable>(any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val baseUrl = "baseUrl"
            val endpoint = "endpoint"
            val timeout = 1L

            httpClient.get<MockResponse, MockError>(baseUrl, endpoint, timeoutMillis = timeout).single()

            coVerify { httpClientProvider.get(baseUrl, endpoint, emptyList(), emptyList(), MockResponse::class, MockError::class, timeout) }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `emits success result with response on successful get`() {
        val response = MockResponse()
        coEvery { httpClientProvider.get<MockResponse, MockError>(any(), any(), any(), any(), any(), any(), any()) } returns response

        runBlockingTest {
            val result = httpClient.get<MockResponse, MockError>("baseUrl", "endpoint").single()

            assertTrue(result.isSuccess, "Expected GET result to be a success")
            assertEquals(response, result.getOrThrow())
        }
    }

    @Test
    fun `emits failure result with data if get failed`() {
        coEvery { httpClientProvider.get<Any, Throwable>(any(), any(), any(), any(), any(), any(), any()) } throws IOException()

        runBlockingTest {
            val result = httpClient.get<Any, MockError>("baseUrl", "endpoint").single()

            assertTrue(result.isFailure, "Expected GET result to be a failure")
        }
    }

    @Test
    fun `makes HTTP POST call to specified endpoint`() {
        coEvery { httpClientProvider.post<Any, Any, Throwable>(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val baseUrl = "baseUrl"
            val endpoint = "endpoint"

            httpClient.post<MockRequest, MockResponse, MockError>(baseUrl, endpoint).single()

            coVerify {
                httpClientProvider.post(
                    baseUrl,
                    endpoint,
                    emptyList(),
                    emptyList(),
                    null,
                    MockRequest::class,
                    MockResponse::class,
                    MockError::class,
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP POST call to specified endpoint with body`() {
        coEvery { httpClientProvider.post<Any, Any, Throwable>(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val baseUrl = "baseUrl"
            val endpoint = "endpoint"
            val body = MockRequest()

            httpClient.post<MockRequest, MockResponse, MockError>(baseUrl, endpoint, body = body).single()

            coVerify {
                httpClientProvider.post(
                    baseUrl,
                    endpoint,
                    emptyList(),
                    emptyList(),
                    body,
                    MockRequest::class,
                    MockResponse::class,
                    MockError::class,
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP POST call to specified endpoint with custom headers`() {
        coEvery { httpClientProvider.post<Any, Any, Throwable>(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val baseUrl = "baseUrl"
            val endpoint = "endpoint"
            val headers = listOf(ApplicationJson())

            httpClient.post<MockRequest, MockResponse, MockError>(baseUrl, endpoint, headers = headers).single()

            coVerify {
                httpClientProvider.post(
                    baseUrl,
                    endpoint,
                    headers,
                    emptyList(),
                    null,
                    MockRequest::class,
                    MockResponse::class,
                    MockError::class,
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP POST call to specified endpoint with custom parameters`() {
        coEvery { httpClientProvider.post<Any, Any, Throwable>(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val baseUrl = "baseUrl"
            val endpoint = "endpoint"
            val parameters = listOf(HttpParameter("key", "value"))

            httpClient.post<MockRequest, MockResponse, MockError>(baseUrl, endpoint, parameters = parameters).single()

            coVerify {
                httpClientProvider.post(
                    baseUrl,
                    endpoint,
                    emptyList(),
                    parameters,
                    null,
                    MockRequest::class,
                    MockResponse::class,
                    MockError::class,
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP POST call to specified endpoint with custom timeout`() {
        coEvery { httpClientProvider.post<Any, Any, Throwable>(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val baseUrl = "baseUrl"
            val endpoint = "endpoint"
            val timeout = 1L

            httpClient.post<MockRequest, MockResponse, MockError>(baseUrl, endpoint, timeoutMillis = timeout).single()

            coVerify {
                httpClientProvider.post(
                    baseUrl,
                    endpoint,
                    emptyList(),
                    emptyList(),
                    null,
                    MockRequest::class,
                    MockResponse::class,
                    MockError::class,
                    timeout,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `emits success result with response on successful post`() {
        val response = MockResponse()
        coEvery { httpClientProvider.post<Any, Any, MockError>(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns response

        runBlockingTest {
            val result = httpClient.post<Any, MockResponse, MockError>("baseUrl", "endpoint").single()

            assertTrue(result.isSuccess, "Expected POST result to be a success")
            assertEquals(response, result.getOrThrow())
        }
    }

    @Test
    fun `emits failure result with data if post failed`() {
        coEvery { httpClientProvider.post<Any, Any, Throwable>(any(), any(), any(), any(), any(), any(), any(), any(), any()) } throws IOException()

        runBlockingTest {
            val result = httpClient.post<Any, Any, Throwable>("baseUrl", "endpoint").single()

            assertTrue(result.isFailure, "Expected POST result to be a failure")
        }
    }

    @Test
    fun `makes HTTP PUT call to specified endpoint`() {
        coEvery { httpClientProvider.put<Any, Any, Throwable>(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val baseUrl = "baseUrl"
            val endpoint = "endpoint"

            httpClient.put<MockRequest, MockResponse, MockError>(baseUrl, endpoint).single()

            coVerify {
                httpClientProvider.put(
                    baseUrl,
                    endpoint,
                    emptyList(),
                    emptyList(),
                    null,
                    MockRequest::class,
                    MockResponse::class,
                    MockError::class,
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP PUT call to specified endpoint with body`() {
        coEvery { httpClientProvider.put<Any, Any, Throwable>(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val baseUrl = "baseUrl"
            val endpoint = "endpoint"
            val body = MockRequest()

            httpClient.put<MockRequest, MockResponse, MockError>(baseUrl, endpoint, body = body).single()

            coVerify {
                httpClientProvider.put(
                    baseUrl,
                    endpoint,
                    emptyList(),
                    emptyList(),
                    body,
                    MockRequest::class,
                    MockResponse::class,
                    MockError::class,
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP PUT call to specified endpoint with custom headers`() {
        coEvery { httpClientProvider.put<Any, Any, Throwable>(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val baseUrl = "baseUrl"
            val endpoint = "endpoint"
            val headers = listOf(ApplicationJson())

            httpClient.put<MockRequest, MockResponse, MockError>(baseUrl, endpoint, headers = headers).single()

            coVerify {
                httpClientProvider.put(
                    baseUrl,
                    endpoint,
                    headers,
                    emptyList(),
                    null,
                    MockRequest::class,
                    MockResponse::class,
                    MockError::class,
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP PUT call to specified endpoint with custom parameters`() {
        coEvery { httpClientProvider.put<Any, Any, Throwable>(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val baseUrl = "baseUrl"
            val endpoint = "endpoint"
            val parameters = listOf(HttpParameter("key", "value"))

            httpClient.put<MockRequest, MockResponse, MockError>(baseUrl, endpoint, parameters = parameters).single()

            coVerify {
                httpClientProvider.put(
                    baseUrl,
                    endpoint,
                    emptyList(),
                    parameters,
                    null,
                    MockRequest::class,
                    MockResponse::class,
                    MockError::class,
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP PUT call to specified endpoint with custom timeout`() {
        coEvery { httpClientProvider.put<Any, Any, Throwable>(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit

        runBlockingTest {
            val baseUrl = "baseUrl"
            val endpoint = "endpoint"
            val timeout = 1L

            httpClient.put<MockRequest, MockResponse, MockError>(baseUrl, endpoint, timeoutMillis = timeout).single()

            coVerify {
                httpClientProvider.put(
                    baseUrl,
                    endpoint,
                    emptyList(),
                    emptyList(),
                    null,
                    MockRequest::class,
                    MockResponse::class,
                    MockError::class,
                    timeout,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `emits success result with response on successful put`() {
        val response = MockResponse()
        coEvery { httpClientProvider.put<Any, Any, Throwable>(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns response

        runBlockingTest {
            val result = httpClient.put<Any, MockResponse, MockError>("baseUrl", "endpoint").single()

            assertTrue(result.isSuccess, "Expected PUT result to be a success")
            assertEquals(response, result.getOrThrow())
        }
    }

    @Test
    fun `emits failure result with data if put failed`() {
        coEvery { httpClientProvider.put<Any, Any, Throwable>(any(), any(), any(), any(), any(), any(), any(), any(), any()) } throws IOException()

        runBlockingTest {
            val result = httpClient.put<Any, Any, MockError>("baseUrl", "endpoint").single()

            assertTrue(result.isFailure, "Expected PUT result to be a failure")
        }
    }

    private data class MockRequest(val body: String = "body")
    private data class MockResponse(val content: String = "content")
    private data class MockError(val description: String = "description") : Throwable()
}