package it.airgap.beaconsdk.core.internal.network

import it.airgap.beaconsdk.core.network.data.HttpHeader
import it.airgap.beaconsdk.core.network.data.HttpParameter
import it.airgap.beaconsdk.core.network.provider.HttpProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass

internal class HttpClientDeprecated(private val httpProvider: HttpProvider) : HttpClient {

    override fun <Response : Any, Error : Throwable> get(
        baseUrl: String,
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        responseClass: KClass<Response>,
        errorClass: KClass<Error>,
        timeoutMillis: Long?,
    ): Flow<Result<Response>> = resultFlowFor {
        httpProvider.get(
            baseUrl,
            endpoint,
            headers,
            parameters,
            responseClass,
            errorClass,
            timeoutMillis,
        )
    }

    override fun <Request : Any, Response : Any, Error : Throwable> post(
        baseUrl: String,
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        body: Request?,
        bodyClass: KClass<Request>,
        responseClass: KClass<Response>,
        errorClass: KClass<Error>,
        timeoutMillis: Long?,
    ): Flow<Result<Response>> = resultFlowFor {
        httpProvider.post(
            baseUrl,
            endpoint,
            headers,
            parameters,
            body,
            bodyClass,
            responseClass,
            errorClass,
            timeoutMillis,
        )
    }

    override fun <Request : Any, Response : Any, Error : Throwable> put(
        baseUrl: String,
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        body: Request?,
        bodyClass: KClass<Request>,
        responseClass: KClass<Response>,
        errorClass: KClass<Error>,
        timeoutMillis: Long?,
    ): Flow<Result<Response>> = resultFlowFor {
        httpProvider.put(
            baseUrl,
            endpoint,
            headers,
            parameters,
            body,
            bodyClass,
            responseClass,
            errorClass,
            timeoutMillis,
        )
    }

    private fun <T> resultFlowFor(httpAction: suspend () -> T): Flow<Result<T>> = flow {
        try {
            emit(Result.success(httpAction()))
        } catch (e: CancellationException) {
            /* no action */
        } catch (e: Exception) {
            emit(Result.failure<T>(e))
        }
    }.take(1)

}