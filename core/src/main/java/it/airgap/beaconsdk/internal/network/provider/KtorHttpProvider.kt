package it.airgap.beaconsdk.internal.network.provider

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import it.airgap.beaconsdk.internal.utils.decodeFromString
import it.airgap.beaconsdk.internal.utils.logDebug
import it.airgap.beaconsdk.network.data.HttpHeader
import it.airgap.beaconsdk.network.data.HttpParameter
import it.airgap.beaconsdk.network.provider.HttpProvider
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

internal class KtorHttpProvider : HttpProvider {
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

    private val json: Json = Json {
        classDiscriminator = "serializationType"
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any, E : Throwable> get(
        baseUrl: String,
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        resourceClass: KClass<T>,
        errorClass: KClass<E>,
        timeoutMillis: Long?,
    ): T = request(HttpMethod.Get, baseUrl, endpoint, headers, parameters, resourceClass, errorClass, timeoutMillis)

    override suspend fun <T : Any, R : Any, E : Throwable> post(
        baseUrl: String,
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        body: T?,
        bodyClass: KClass<T>,
        responseClass: KClass<R>,
        errorClass: KClass<E>,
        timeoutMillis: Long?,
    ): R = request(HttpMethod.Post, baseUrl, endpoint, headers, parameters, responseClass, errorClass, timeoutMillis) {
        body?.let { this.body = it }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any, R : Any, E : Throwable> put(
        baseUrl: String,
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        body: T?,
        bodyClass: KClass<T>,
        responseClass: KClass<R>,
        errorClass: KClass<E>,
        timeoutMillis: Long?,
    ): R = request(HttpMethod.Put, baseUrl, endpoint, headers, parameters, responseClass, errorClass, timeoutMillis) {
        body?.let { this.body = it }
    }

    private suspend inline fun <T : Any, E : Throwable> request(
        method: HttpMethod,
        baseUrl: String,
        endpoint: String,
        httpHeaders: List<HttpHeader>,
        httpParameters: List<HttpParameter>,
        responseClass: KClass<T>,
        errorClass: KClass<E>,
        timeoutMillis: Long?,
        block: HttpRequestBuilder.() -> Unit = {},
    ): T {
        try {
            val response = ktorClient.request<String> {
                this.method = method
                url(apiUrl(baseUrl, endpoint))
                headers(httpHeaders)
                parameters(httpParameters)

                timeoutMillis?.let { timeout(it) }

                block(this)
            }

            return decodeResponse(response, responseClass)
        } catch (e: ClientRequestException) {
            throw decodeError(e, errorClass)
        }
    }

    private fun <T : Any> decodeResponse(response: String, responseClass: KClass<T>): T =
        json.decodeFromString(response, responseClass)

    private suspend fun <T : Throwable> decodeError(error: ClientRequestException, errorClass: KClass<T>): Throwable =
        try {
            val response = error.response.readText(Charsets.UTF_8)
            json.decodeFromString(response, errorClass)
        } catch (e: Exception) {
            error
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