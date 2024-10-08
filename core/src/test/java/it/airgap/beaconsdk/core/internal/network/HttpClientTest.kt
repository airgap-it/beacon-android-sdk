package it.airgap.beaconsdk.core.internal.network

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.internal.compat.CoreCompat
import it.airgap.beaconsdk.core.internal.network.HttpClient.Companion.get
import it.airgap.beaconsdk.core.internal.network.HttpClient.Companion.post
import it.airgap.beaconsdk.core.internal.network.HttpClient.Companion.put
import it.airgap.beaconsdk.core.internal.network.data.ApplicationJson
import it.airgap.beaconsdk.core.internal.serializer.coreJson
import it.airgap.beaconsdk.core.network.data.HttpParameter
import it.airgap.beaconsdk.core.network.provider.HttpClientProvider
import it.airgap.beaconsdk.core.scope.BeaconScope
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mockDependencyRegistry
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class HttpClientTest {

    @MockK
    private lateinit var httpClientProvider: HttpClientProvider

    private lateinit var httpClient: HttpClient
    private lateinit var json: Json

    private val beaconScope: BeaconScope = BeaconScope.Global

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        val dependencyRegistry = mockDependencyRegistry()

        json = Json(from = coreJson(dependencyRegistry.blockchainRegistry, CoreCompat(beaconScope))) {
            prettyPrint = true
        }
        httpClient = HttpClientImpl(httpClientProvider, json)
    }

    @Test
    fun `makes HTTP GET call to specified endpoint`() {
        coEvery { httpClientProvider.get(any(), any(), any(), any(), any()) } returns ""

        runBlockingTest {
            val baseUrl = "baseUrl"
            val endpoint = "endpoint"

            httpClient.get<MockResponse, MockError>(baseUrl, endpoint).single()

            coVerify { httpClientProvider.get(baseUrl, endpoint, emptyList(), emptyList(), null) }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP GET call to specified endpoint with custom headers`() {
        coEvery { httpClientProvider.get(any(), any(), any(), any(), any()) } returns ""

        runBlockingTest {
            val baseUrl = "baseUrl"
            val endpoint = "endpoint"
            val headers = listOf(ApplicationJson())

            httpClient.get<MockResponse, MockError>(baseUrl, endpoint, headers = headers).single()

            coVerify { httpClientProvider.get(baseUrl, endpoint, headers, emptyList(), null) }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP GET call to specified endpoint with custom parameters`() {
        coEvery { httpClientProvider.get(any(), any(), any(), any(), any()) } returns ""

        runBlockingTest {
            val baseUrl = "baseUrl"
            val endpoint = "endpoint"
            val parameters = listOf(HttpParameter("key", "value"))

            httpClient.get<MockResponse, MockError>(baseUrl, endpoint, parameters = parameters).single()

            coVerify { httpClientProvider.get(baseUrl, endpoint, emptyList(), parameters, null) }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP GET call to specified endpoint with custom timeout`() {
        coEvery { httpClientProvider.get(any(), any(), any(), any(), any()) } returns ""

        runBlockingTest {
            val baseUrl = "baseUrl"
            val endpoint = "endpoint"
            val timeout = 1L

            httpClient.get<MockResponse, MockError>(baseUrl, endpoint, timeoutMillis = timeout).single()

            coVerify { httpClientProvider.get(baseUrl, endpoint, emptyList(), emptyList(), timeout) }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `emits success result with response on successful get`() {
        val response = MockResponse()
        coEvery { httpClientProvider.get(any(), any(), any(), any(), any()) } returns json.encodeToString(response)

        runBlockingTest {
            val result = httpClient.get<MockResponse, MockError>("baseUrl", "endpoint").single()

            assertTrue(result.isSuccess, "Expected GET result to be a success")
            assertEquals(response, result.getOrThrow())
        }
    }

    @Test
    fun `emits failure result with data if get failed`() {
        coEvery { httpClientProvider.get(any(), any(), any(), any(), any()) } throws IOException()

        runBlockingTest {
            val result = httpClient.get<Any, MockError>("baseUrl", "endpoint").single()

            assertTrue(result.isFailure, "Expected GET result to be a failure")
        }
    }

    @Test
    fun `makes HTTP POST call to specified endpoint`() {
        coEvery { httpClientProvider.post(any(), any(), any(), any(), any(), any()) } returns ""

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
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP POST call to specified endpoint with body`() {
        coEvery { httpClientProvider.post(any(), any(), any(), any(), any(), any()) } returns ""

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
                    json.encodeToString(body),
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP POST call to specified endpoint with custom headers`() {
        coEvery { httpClientProvider.post(any(), any(), any(), any(), any(), any()) } returns ""

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
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP POST call to specified endpoint with custom parameters`() {
        coEvery { httpClientProvider.post(any(), any(), any(), any(), any(), any()) } returns ""

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
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP POST call to specified endpoint with custom timeout`() {
        coEvery { httpClientProvider.post(any(), any(), any(), any(), any(), any()) } returns ""

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
                    timeout,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `emits success result with response on successful post`() {
        val response = MockResponse()
        coEvery { httpClientProvider.post(any(), any(), any(), any(), any(), any()) } returns json.encodeToString(response)

        runBlockingTest {
            val result = httpClient.post<Any, MockResponse, MockError>("baseUrl", "endpoint").single()

            assertTrue(result.isSuccess, "Expected POST result to be a success")
            assertEquals(response, result.getOrThrow())
        }
    }

    @Test
    fun `emits failure result with data if post failed`() {
        coEvery { httpClientProvider.post(any(), any(), any(), any(), any(), any()) } throws IOException()

        runBlockingTest {
            val result = httpClient.post<Any, Any, Throwable>("baseUrl", "endpoint").single()

            assertTrue(result.isFailure, "Expected POST result to be a failure")
        }
    }

    @Test
    fun `makes HTTP PUT call to specified endpoint`() {
        coEvery { httpClientProvider.put(any(), any(), any(), any(), any(), any()) } returns ""

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
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP PUT call to specified endpoint with body`() {
        coEvery { httpClientProvider.put(any(), any(), any(), any(), any(), any()) } returns ""

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
                    json.encodeToString(body),
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP PUT call to specified endpoint with custom headers`() {
        coEvery { httpClientProvider.put(any(), any(), any(), any(), any(), any()) } returns ""

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
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP PUT call to specified endpoint with custom parameters`() {
        coEvery { httpClientProvider.put(any(), any(), any(), any(), any(), any()) } returns ""

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
                    null,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `makes HTTP PUT call to specified endpoint with custom timeout`() {
        coEvery { httpClientProvider.put(any(), any(), any(), any(), any(), any()) } returns ""

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
                    timeout,
                )
            }
            confirmVerified(httpClientProvider)
        }
    }

    @Test
    fun `emits success result with response on successful put`() {
        val response = MockResponse()
        coEvery { httpClientProvider.put(any(), any(), any(), any(), any(), any()) } returns json.encodeToString(response)

        runBlockingTest {
            val result = httpClient.put<Any, MockResponse, MockError>("baseUrl", "endpoint").single()

            assertTrue(result.isSuccess, "Expected PUT result to be a success")
            assertEquals(response, result.getOrThrow())
        }
    }

    @Test
    fun `emits failure result with data if put failed`() {
        coEvery { httpClientProvider.put(any(), any(), any(), any(), any(), any()) } throws IOException()

        runBlockingTest {
            val result = httpClient.put<Any, Any, MockError>("baseUrl", "endpoint").single()

            assertTrue(result.isFailure, "Expected PUT result to be a failure")
        }
    }

    @Serializable
    private data class MockRequest(val body: String = "body")

    @Serializable
    private data class MockResponse(val content: String = "content")

    @Serializable
    private data class MockError(val description: String = "description") : Throwable()
}