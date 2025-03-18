package it.airgap.beaconsdk.core.internal.utils

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.exception.UnknownException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Result.Companion.success(): Result<Unit> = success(Unit)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> Result.Companion.failure(exception: Throwable? = null): Result<T> = failure(exception ?: UnknownException())

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <T, S> Result<T>.flatMap(transform: (T) -> Result<S>): Result<S> =
    runCatchingFlat { transform(getOrThrow()) }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <T> Result<T>.mapException(transform: (Throwable) -> Throwable): Result<T> =
    fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(transform(it)) },
    )
