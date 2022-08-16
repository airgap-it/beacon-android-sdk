package it.airgap.beaconsdk.core.internal.utils

import androidx.annotation.RestrictTo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> Flow<Result<T>>.onEachSuccess(action: suspend (T) -> Unit): Flow<Result<T>> =
    onEach { result -> result.onSuccess { action(it) } }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> Flow<Result<T>>.onEachFailure(action: suspend (Throwable) -> Unit): Flow<Result<T>> =
    onEach { result -> result.onFailure { action(it) } }