package it.airgap.beaconsdk.core.internal.network.provider

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import it.airgap.beaconsdk.core.internal.utils.decodeFromString
import it.airgap.beaconsdk.core.internal.utils.logDebug
import it.airgap.beaconsdk.core.network.data.HttpHeader
import it.airgap.beaconsdk.core.network.data.HttpParameter
import it.airgap.beaconsdk.core.network.exception.HttpException
import it.airgap.beaconsdk.core.network.provider.HttpClientProvider
import it.airgap.beaconsdk.core.network.provider.HttpProvider
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.reflect.KClass

internal class KtorHttpClientProvider(private val json: Json) : HttpClientProvider {
    private val ktorClient by lazy {
        HttpClient(OkHttp) {
            install(JsonFeature) {
                serializer = KotlinxSerializer(json)
            }

            install(HttpTimeout)

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        logDebug(TAG, message)
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
        body?.let { setBodyAsText(it) }
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
        body?.let { setBodyAsText(it) }
    }

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

                timeoutMillis?.let { timeout(it) }

                block(this)
            }
        } catch (e: ClientRequestException) {
            throw decodeError(e)
        }

    private suspend fun decodeError(error: ClientRequestException): Throwable =
        try {
            val response = error.response.readText(Charsets.UTF_8)
            HttpException.Serialized(response)
        } catch (e: Exception) {
            HttpException.Failure(error.response.status.value, cause = error)
        }

    private fun apiUrl(baseUrl: String, endpoint: String): String = "$baseUrl/${endpoint.trimStart('/')}"

    private fun HttpRequestBuilder.setBodyAsText(body: String) {
        this.body = json.decodeFromString<JsonElement>(body)
    }


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