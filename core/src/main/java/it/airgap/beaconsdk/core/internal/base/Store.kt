package it.airgap.beaconsdk.core.internal.base

import androidx.annotation.RestrictTo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class Store<S, A> {
    protected val stateMutex: Mutex = Mutex()

    public suspend fun state(): Result<S> = stateMutex.withLock { stateLocked() }
    protected abstract suspend fun stateLocked(): Result<S>

    public suspend fun intent(action: A): Result<Unit> = stateMutex.withLock { intentLocked(action) }

    protected abstract suspend fun intentLocked(action: A): Result<Unit>
}
