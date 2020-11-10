package it.airgap.beaconsdk.internal.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn

internal class Poller {
    fun <T> poll(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        interval: Long = 0,
        action: suspend () -> InternalResult<T>,
    ): Flow<InternalResult<T>> =
        channelFlow {
            while (!isClosedForSend) {
                val response = flatTryResult { action() }
                send(response)
                delay(interval)
            }
        }.flowOn(dispatcher)
}