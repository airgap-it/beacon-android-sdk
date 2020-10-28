package it.airgap.beaconsdk.internal.network

import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.flatTryResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlin.coroutines.CoroutineContext

//@ExperimentalCoroutinesApi
internal class HttpPoller {
    fun <T> poll(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        interval: Long = 0,
        action: suspend () -> InternalResult<T>
    ): Flow<InternalResult<T>> =
        channelFlow {
            while (!isClosedForSend) {
                val response = flatTryResult { action() }
                send(response)
                delay(interval)
            }
        }.flowOn(dispatcher)
}