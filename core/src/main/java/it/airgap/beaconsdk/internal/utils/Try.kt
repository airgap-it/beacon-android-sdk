package it.airgap.beaconsdk.internal.utils

import androidx.annotation.IntRange

internal inline fun <T> runCatchingFlat(block: () -> Result<T>): Result<T> =
    try {
        block()
    } catch (e: Exception) {
        Result.failure(e)
    }

internal inline fun <T> tryLog(tag: String, block: () -> T): T? =
    try {
        block()
    } catch (e: Exception) {
        logError(tag, e)
        null
    }

internal inline fun <T> runCatchingRepeat(@IntRange(from = 1) times: Int, action: () -> T): Result<T> =
    runCatchingFlatRepeat(times) { runCatching(action) }

internal inline fun <T> runCatchingFlatRepeat(@IntRange(from = 1) times: Int, action: () -> Result<T>): Result<T> {
    require(times > 0)

    var result: Result<T>
    var counter = times

    do {
        result = action()
        if (result.isSuccess) break
        if (result.isFailure) counter--
    } while (counter > 0)

    return result
}