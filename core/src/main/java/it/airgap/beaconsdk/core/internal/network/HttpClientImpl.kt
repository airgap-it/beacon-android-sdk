package it.airgap.beaconsdk.core.internal.network

import it.airgap.beaconsdk.core.internal.utils.decodeFromString
import it.airgap.beaconsdk.core.internal.utils.encodeToString
import it.airgap.beaconsdk.core.network.data.HttpHeader
import it.airgap.beaconsdk.core.network.data.HttpParameter
import it.airgap.beaconsdk.core.network.exception.HttpException
import it.airgap.beaconsdk.core.network.provider.HttpClientProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.serialization.json.Json
import java.util.concurrent.CancellationException
import kotlin.reflect.KClass

internal class HttpClientImpl(private val httpClientProvider: HttpClientProvider, json: Json) : HttpClient {

    private val json: Json = Json(from = json) {
        prettyPrint = true
    }

    override fun <Response : Any, Error : Throwable> get(
        baseUrl: String,
        endpoint: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        responseClass: KClass<Response>,
        errorClass: KClass<Error>,
        timeoutMillis: Long?,
    ): Flow<Result<Response>> = resultFlowFor(errorClass) {
        httpClientProvider.get(
            baseUrl,
            endpoint,
            headers,
            parameters,
            timeoutMillis,
        ).decodeAsResponse(responseClass)
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
    ): Flow<Result<Response>> = resultFlowFor(errorClass) {
        httpClientProvider.post(
            baseUrl,
            endpoint,
            headers,
            parameters,
            body?.encodeAsRequest(bodyClass),
            timeoutMillis,
        ).decodeAsResponse(responseClass)
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
    ): Flow<Result<Response>> = resultFlowFor(errorClass) {
        httpClientProvider.put(
            baseUrl,
            endpoint,
            headers,
            parameters,
            body?.encodeAsRequest(bodyClass),
            timeoutMillis,
        ).decodeAsResponse(responseClass)
    }

    fun <T, E : Throwable> resultFlowFor(errorClass: KClass<E>, httpAction: suspend () -> T): Flow<Result<T>> = flow {
        try {
            emit(Result.success(httpAction()))
        } catch (e: CancellationException) {
            /* no action */
        } catch (e: HttpException) {
            when (e) {
                is HttpException.Serialized -> emitFailure<T>(e.body.decodeAsResponse(errorClass))
                is HttpException.Failure -> emitFailure<T>(e)
            }
        } catch (e: Exception) {
            emitFailure<T>(e)
        }
    }.take(1)

    private fun <T : Any> T.encodeAsRequest(requestClass: KClass<T>): String = json.encodeToString(this, requestClass)

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> String.decodeAsResponse(responseClass: KClass<T>): T =
        when (responseClass) {
            Unit::class -> Unit as T
            else -> json.decodeFromString(this, responseClass)
        }

    private suspend fun <T> FlowCollector<Result<T>>.emitFailure(e: Throwable) {
        emit(Result.failure(e))
    }
}