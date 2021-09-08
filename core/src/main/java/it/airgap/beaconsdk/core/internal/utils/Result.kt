package it.airgap.beaconsdk.core.internal.utils

import it.airgap.beaconsdk.core.exception.UnknownException

internal fun Result.Companion.success(): Result<Unit> = success(Unit)
internal fun <T> Result.Companion.failure(): Result<T> = failure(UnknownException())

internal inline fun <T, S> Result<T>.flatMap(transform: (T) -> Result<S>): Result<S> =
    runCatchingFlat { transform(getOrThrow()) }

internal inline fun <T> Result<T>.mapException(transform: (Throwable) -> Throwable): Result<T> =
    fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(transform(it)) },
    )
