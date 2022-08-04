package it.airgap.beaconsdk.core.internal.network

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.network.data.HttpHeader
import it.airgap.beaconsdk.core.network.data.HttpParameter
import it.airgap.beaconsdk.core.network.exception.HttpException
import it.airgap.beaconsdk.core.network.provider.HttpClientProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.CancellationException
import kotlin.reflect.KClass

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface HttpClient {

    public fun <Response : Any, Error : Throwable> get(
        baseUrl: String,
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        responseClass: KClass<Response>,
        errorClass: KClass<Error>,
        timeoutMillis: Long?,
    ): Flow<Result<Response>>

    public fun <Request : Any, Response : Any, Error : Throwable> post(
        baseUrl: String,
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        body: Request? = null,
        bodyClass: KClass<Request>,
        responseClass: KClass<Response>,
        errorClass: KClass<Error>,
        timeoutMillis: Long?,
    ): Flow<Result<Response>>

    public fun <Request : Any, Response : Any, Error : Throwable> put(
        baseUrl: String,
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        body: Request? = null,
        bodyClass: KClass<Request>,
        responseClass: KClass<Response>,
        errorClass: KClass<Error>,
        timeoutMillis: Long?,
    ): Flow<Result<Response>>

    public companion object {
        public inline fun <reified Response : Any, reified Error : Throwable> HttpClient.get(
            baseUrl: String,
            endpoint: String,
            headers: List<HttpHeader> = emptyList(),
            parameters: List<HttpParameter> = emptyList(),
            timeoutMillis: Long? = null,
        ): Flow<Result<Response>> = get(baseUrl, endpoint, headers, parameters, Response::class, Error::class, timeoutMillis)

        public inline fun <reified Request : Any, reified Response : Any, reified Error: Throwable> HttpClient.post(
            baseUrl: String,
            endpoint: String,
            body: Request? = null,
            headers: List<HttpHeader> = emptyList(),
            parameters: List<HttpParameter> = emptyList(),
            timeoutMillis: Long? = null,
        ): Flow<Result<Response>> = post(baseUrl, endpoint, headers, parameters, body, Request::class, Response::class, Error::class, timeoutMillis)

        public inline fun <reified Request : Any, reified Response : Any, reified Error : Throwable> HttpClient.put(
            baseUrl: String,
            endpoint: String,
            body: Request? = null,
            headers: List<HttpHeader> = emptyList(),
            parameters: List<HttpParameter> = emptyList(),
            timeoutMillis: Long? = null,
        ): Flow<Result<Response>> = put(baseUrl, endpoint, headers, parameters, body, Request::class, Response::class, Error::class, timeoutMillis)
    }
}

public fun HttpClient(httpClientProvider: HttpClientProvider, json: Json): HttpClient = HttpClientImpl(httpClientProvider, json)