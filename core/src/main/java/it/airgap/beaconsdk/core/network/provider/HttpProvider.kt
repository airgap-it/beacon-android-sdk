package it.airgap.beaconsdk.core.network.provider

import it.airgap.beaconsdk.core.network.data.HttpHeader
import it.airgap.beaconsdk.core.network.data.HttpParameter
import kotlin.reflect.KClass

public interface HttpProvider {

    @Throws(Exception::class)
    public suspend fun <T : Any, E : Throwable> get(
        baseUrl: String,
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        resourceClass: KClass<T>,
        errorClass: KClass<E>,
        timeoutMillis: Long?,
    ): T

    @Throws(Exception::class)
    public suspend fun <T : Any, R : Any, E : Throwable> post(
        baseUrl: String,
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        body: T? = null,
        bodyClass: KClass<T>,
        responseClass: KClass<R>,
        errorClass: KClass<E>,
        timeoutMillis: Long?,
    ): R

    @Throws(Exception::class)
    public suspend fun <T : Any, R : Any, E : Throwable> put(
        baseUrl: String,
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        body: T? = null,
        bodyClass: KClass<T>,
        responseClass: KClass<R>,
        errorClass: KClass<E>,
        timeoutMillis: Long?,
    ): R
}