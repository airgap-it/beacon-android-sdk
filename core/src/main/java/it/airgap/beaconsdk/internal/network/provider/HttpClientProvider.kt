package it.airgap.beaconsdk.internal.network.provider

import it.airgap.beaconsdk.internal.network.data.HttpHeader
import it.airgap.beaconsdk.internal.network.data.HttpParameter
import kotlin.Throws
import kotlin.reflect.KClass

internal abstract class HttpClientProvider(protected val baseUrl: String) {
    @Throws(Exception::class)
    abstract suspend fun <T : Any> get(
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        resourceClass: KClass<T>,
    ): T

    @Throws(Exception::class)
    abstract suspend fun <T : Any, R : Any> post(
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        body: T? = null,
        bodyClass: KClass<T>,
        responseClass: KClass<R>,
    ): R

    @Throws(Exception::class)
    abstract suspend fun <T : Any, R : Any> put(
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        body: T? = null,
        bodyClass: KClass<T>,
        responseClass: KClass<R>,
    ): R
}