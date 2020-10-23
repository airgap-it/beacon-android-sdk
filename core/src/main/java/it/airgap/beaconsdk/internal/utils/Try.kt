package it.airgap.beaconsdk.internal.utils

internal inline fun <T> tryResult(block: () -> T): InternalResult<T> =
    try {
        internalSuccess(block())
    } catch (e: Exception) {
        internalError(e)
    }

internal inline fun <T> flatTryResult(block: () -> InternalResult<T>): InternalResult<T> =
    try {
        block()
    } catch (e: Exception) {
        internalError(e)
    }

internal inline fun <T> tryLog(tag: String, block: () -> T): T? =
    try {
        block()
    } catch (e: Exception) {
        logError(tag, e)
        null
    }