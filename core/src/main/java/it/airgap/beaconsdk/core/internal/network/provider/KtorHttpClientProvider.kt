package it.airgap.beaconsdk.core.internal.network.provider

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import it.airgap.beaconsdk.core.internal.utils.logDebug
import it.airgap.beaconsdk.core.network.data.HttpHeader
import it.airgap.beaconsdk.core.network.data.HttpParameter
import it.airgap.beaconsdk.core.network.exception.HttpException
import it.airgap.beaconsdk.core.network.provider.HttpClientProvider
import it.airgap.beaconsdk.core.scope.BeaconScope
import kotlinx.serialization.json.Json

internal class KtorHttpClientProvider(private val json: Json, private val beaconScope: BeaconScope) : HttpClientProvider {
    private val ktorClient by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json)
            }

            install(HttpTimeout)

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        logDebug("$TAG\$$beaconScope", message)
                    }
                }
                level = LogLevel.ALL
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun get(
        baseUrl: String,
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        timeoutMillis: Long?,
    ): String = request(HttpMethod.Get, baseUrl, endpoint, headers, parameters, timeoutMillis)

    override suspend fun post(
        baseUrl: String,
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        body: String?,
        timeoutMillis: Long?,
    ): String = request(HttpMethod.Post, baseUrl, endpoint, headers, parameters, timeoutMillis) {
        body?.let { setBody(it) }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun put(
        baseUrl: String,
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        body: String?,
        timeoutMillis: Long?,
    ): String = request(HttpMethod.Put, baseUrl, endpoint, headers, parameters, timeoutMillis) {
        body?.let { setBody(it) }
    }

    @Throws(HttpException::class)
    private suspend fun request(
        method: HttpMethod,
        baseUrl: String,
        endpoint: String,
        httpHeaders: List<HttpHeader>,
        httpParameters: List<HttpParameter>,
        timeoutMillis: Long?,
        block: HttpRequestBuilder.() -> Unit = {},
    ): String =
        try {
            ktorClient.request {
                this.method = method
                url(apiUrl(baseUrl, endpoint))
                headers(httpHeaders)
                parameters(httpParameters)

                expectSuccess = true

                timeoutMillis?.let { timeout(it) }

                block(this)
            }.body()
        } catch (e: ClientRequestException) {
            throw decodeError(e)
        }

    private suspend fun decodeError(error: ClientRequestException): HttpException =
        try {
            val response = error.response.body<String>()
            HttpException.Serialized(response)
        } catch (e: Exception) {
            HttpException.Failure(error.response.status.value, cause = error)
        }

    private fun apiUrl(baseUrl: String, endpoint: String): String = "$baseUrl/${endpoint.trimStart('/')}"

    private fun HttpRequestBuilder.headers(headers: List<HttpHeader>) {
        headers.forEach { header(it.first, it.second) }
    }

    private fun HttpRequestBuilder.parameters(parameters: List<HttpParameter>) {
        parameters.forEach { parameter(it.first, it.second) }
    }

    private fun HttpRequestBuilder.timeout(timeoutMillis: Long?) {
        timeout {
            requestTimeoutMillis = timeoutMillis
            connectTimeoutMillis = timeoutMillis
            socketTimeoutMillis = timeoutMillis
        }
    }

    companion object {
        const val TAG = "HttpClient"
    }
}