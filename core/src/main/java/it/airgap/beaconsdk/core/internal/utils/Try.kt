package it.airgap.beaconsdk.core.internal.utils

import androidx.annotation.IntRange
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <T> runCatchingFlat(block: () -> Result<T>): Result<T> =
    try {
        block()
    } catch (e: Exception) {
        Result.failure(e)
    }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <T> tryLog(tag: String, block: () -> T): T? =
    try {
        block()
    } catch (e: Exception) {
        logError(tag, e)
        null
    }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <T> runCatchingRepeat(@IntRange(from = 1) times: Int, action: () -> T): Result<T> =
    runCatchingFlatRepeat(times) { runCatching(action) }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <T> runCatchingFlatRepeat(@IntRange(from = 1) times: Int, action: () -> Result<T>): Result<T> {
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