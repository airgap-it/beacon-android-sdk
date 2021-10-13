package it.airgap.beaconsdk.core.internal.utils

import androidx.annotation.RestrictTo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn

@OptIn(ExperimentalCoroutinesApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Poller {
    public fun <T> poll(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        interval: Long = 0,
        action: suspend () -> Result<T>,
    ): Flow<Result<T>> =
        channelFlow {
            while (!isClosedForSend) {
                val response = runCatchingFlat { action() }
                send(response)
                delay(interval)
            }
        }.flowOn(dispatcher)
}