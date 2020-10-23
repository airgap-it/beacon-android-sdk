package it.airgap.beaconsdk.internal.network.provider

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import it.airgap.beaconsdk.internal.network.data.HttpHeader
import it.airgap.beaconsdk.internal.network.data.HttpParameter
import it.airgap.beaconsdk.internal.utils.decodeFromString
import it.airgap.beaconsdk.internal.utils.logDebug
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

internal class KtorHttpClientProvider(baseUrl: String) : HttpClientProvider(baseUrl) {
    private val ktorClient by lazy {
        HttpClient(OkHttp) {
            install(JsonFeature) {
                serializer = KotlinxSerializer(json)
            }

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

    private val json: Json = Json { classDiscriminator = "_type" }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> get(
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        resourceClass: KClass<T>,
    ): T = request(HttpMethod.Get, apiUrl(endpoint), headers, parameters, resourceClass)

    override suspend fun <T : Any, R : Any> post(
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        body: T?,
        bodyClass: KClass<T>,
        responseClass: KClass<R>,
    ): R = request(HttpMethod.Post, apiUrl(endpoint), headers, parameters, responseClass) {
        body?.let { this.body = it }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any, R : Any> put(
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        body: T?,
        bodyClass: KClass<T>,
        responseClass: KClass<R>,
    ): R = request(HttpMethod.Put, apiUrl(endpoint), headers, parameters, responseClass) {
        body?.let { this.body = it }
    }

    private suspend inline fun <T : Any> request(
        method: HttpMethod,
        endpoint: String,
        httpHeaders: List<HttpHeader>,
        httpParameters: List<HttpParameter>,
        responseClass: KClass<T>,
        block: HttpRequestBuilder.() -> Unit = {}): T
    {

        val response = ktorClient.request<String> {
            this.method = method
            url(apiUrl(endpoint))
            headers(httpHeaders)
            parameters(httpParameters)
            block(this)
        }

        return json.decodeFromString(response, responseClass)
    }

    private fun apiUrl(endpoint: String): String = "$baseUrl/${endpoint.trimStart('/')}"

    private fun HttpRequestBuilder.headers(headers: List<HttpHeader>) {
        headers.forEach { header(it.first, it.second) }
    }

    private fun HttpRequestBuilder.parameters(parameters: List<HttpParameter>) {
        parameters.forEach { parameter(it.first, it.second) }
    }

    companion object {
        const val TAG = "HttpClient"
    }
}