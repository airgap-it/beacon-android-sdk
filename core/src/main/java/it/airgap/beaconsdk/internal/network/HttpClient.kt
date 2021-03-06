package it.airgap.beaconsdk.internal.network

import it.airgap.beaconsdk.internal.network.data.HttpHeader
import it.airgap.beaconsdk.internal.network.data.HttpParameter
import it.airgap.beaconsdk.internal.network.provider.HttpClientProvider
import it.airgap.beaconsdk.internal.utils.Failure
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.Success
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import java.util.concurrent.CancellationException

internal class HttpClient(private val httpClientProvider: HttpClientProvider) {
    inline fun <reified T : Any> get(
        endpoint: String,
        headers: List<HttpHeader> = emptyList(),
        parameters: List<HttpParameter> = emptyList(),
        timeoutMillis: Long? = null,
    ): Flow<InternalResult<T>> = resultFlowFor {
        httpClientProvider.get(
            endpoint,
            headers,
            parameters,
            T::class,
            timeoutMillis,
        )
    }

    inline fun <reified T : Any, reified R : Any> post(
        endpoint: String,
        body: T? = null,
        headers: List<HttpHeader> = emptyList(),
        parameters: List<HttpParameter> = emptyList(),
        timeoutMillis: Long? = null,
    ): Flow<InternalResult<R>> = resultFlowFor {
        httpClientProvider.post(
            endpoint,
            headers,
            parameters,
            body,
            T::class,
            R::class,
            timeoutMillis,
        )
    }

    inline fun <reified T : Any, reified R : Any> put(
        endpoint: String,
        body: T? = null,
        headers: List<HttpHeader> = emptyList(),
        parameters: List<HttpParameter> = emptyList(),
        timeoutMillis: Long? = null,
    ): Flow<InternalResult<R>> = resultFlowFor {
        httpClientProvider.put(
            endpoint,
            headers,
            parameters,
            body,
            T::class,
            R::class,
            timeoutMillis,
        )
    }

    private fun <T> resultFlowFor(httpAction: suspend () -> T): Flow<InternalResult<T>> = flow {
        try {
            emit(Success(httpAction()))
        } catch (e: CancellationException) {
            /* no action */
        } catch (e: Exception) {
            emit(Failure<T>(e))
        }
    }.take(1)
}