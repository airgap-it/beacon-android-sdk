package it.airgap.beaconsdk.core.network.provider

import it.airgap.beaconsdk.core.network.data.HttpHeader
import it.airgap.beaconsdk.core.network.data.HttpParameter
import it.airgap.beaconsdk.core.network.exception.HttpException
import kotlin.reflect.KClass

public interface HttpClientProvider {

    @Throws(HttpException::class)
    public suspend fun get(
        baseUrl: String,
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        timeoutMillis: Long?,
    ): String

    @Throws(HttpException::class)
    public suspend fun post(
        baseUrl: String,
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        body: String? = null,
        timeoutMillis: Long?,
    ): String

    @Throws(HttpException::class)
    public suspend fun put(
        baseUrl: String,
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        body: String? = null,
        timeoutMillis: Long?,
    ): String
}