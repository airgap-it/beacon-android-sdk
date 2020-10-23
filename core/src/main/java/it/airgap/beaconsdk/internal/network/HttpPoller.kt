package it.airgap.beaconsdk.internal.network

import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.flatTryResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlin.coroutines.CoroutineContext

//@ExperimentalCoroutinesApi
internal class HttpPoller {

    inline fun <T> poll(
        context: CoroutineContext,
        interval: Long = 0,
        crossinline action: suspend () -> InternalResult<T>
    ): Flow<InternalResult<T>> =
        channelFlow {
            while (!isClosedForSend) {
                val response = flatTryResult { action() }
                send(response)
                delay(interval)
            }
        }.flowOn(context)

}