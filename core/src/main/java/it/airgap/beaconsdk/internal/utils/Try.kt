package it.airgap.beaconsdk.internal.utils

internal inline fun <T> tryResult(block: () -> T): InternalResult<T> =
    try {
        Success(block())
    } catch (e: Exception) {
        Failure(e)
    }

internal inline fun <T> flatTryResult(block: () -> InternalResult<T>): InternalResult<T> =
    try {
        block()
    } catch (e: Exception) {
        Failure(e)
    }

internal inline fun <T> tryLog(tag: String, block: () -> T): T? =
    try {
        block()
    } catch (e: Exception) {
        logError(tag, e)
        null
    }