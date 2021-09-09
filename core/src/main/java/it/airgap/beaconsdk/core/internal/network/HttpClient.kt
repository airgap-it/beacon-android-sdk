package it.airgap.beaconsdk.core.internal.network

import it.airgap.beaconsdk.core.network.data.HttpHeader
import it.airgap.beaconsdk.core.network.data.HttpParameter
import it.airgap.beaconsdk.core.network.provider.HttpProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import java.util.concurrent.CancellationException

public class HttpClient(public val httpProvider: HttpProvider) {
    inline fun <reified T : Any, reified E : Throwable> get(
        baseUrl: String,
        endpoint: String,
        headers: List<HttpHeader> = emptyList(),
        parameters: List<HttpParameter> = emptyList(),
        timeoutMillis: Long? = null,
    ): Flow<Result<T>> = resultFlowFor {
        httpProvider.get(
            baseUrl,
            endpoint,
            headers,
            parameters,
            T::class,
            E::class,
            timeoutMillis,
        )
    }

    inline fun <reified T : Any, reified R : Any, reified E: Throwable> post(
        baseUrl: String,
        endpoint: String,
        body: T? = null,
        headers: List<HttpHeader> = emptyList(),
        parameters: List<HttpParameter> = emptyList(),
        timeoutMillis: Long? = null,
    ): Flow<Result<R>> = resultFlowFor {
        httpProvider.post(
            baseUrl,
            endpoint,
            headers,
            parameters,
            body,
            T::class,
            R::class,
            E::class,
            timeoutMillis,
        )
    }

    inline fun <reified T : Any, reified R : Any, reified E : Throwable> put(
        baseUrl: String,
        endpoint: String,
        body: T? = null,
        headers: List<HttpHeader> = emptyList(),
        parameters: List<HttpParameter> = emptyList(),
        timeoutMillis: Long? = null,
    ): Flow<Result<R>> = resultFlowFor {
        httpProvider.put(
            baseUrl,
            endpoint,
            headers,
            parameters,
            body,
            T::class,
            R::class,
            E::class,
            timeoutMillis,
        )
    }

    public fun <T> resultFlowFor(httpAction: suspend () -> T): Flow<Result<T>> = flow {
        try {
            emit(Result.success(httpAction()))
        } catch (e: CancellationException) {
            /* no action */
        } catch (e: Exception) {
            emit(Result.failure<T>(e))
        }
    }.take(1)
}