package it.airgap.beaconsdk.internal.base

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal abstract class Store<S, A> {
    protected val stateMutex: Mutex = Mutex()

    suspend fun state(): Result<S> = stateMutex.withLock { stateLocked() }
    protected abstract suspend fun stateLocked(): Result<S>

    suspend fun intent(action: A): Result<Unit> = stateMutex.withLock { intentLocked(action) }

    protected abstract suspend fun intentLocked(action: A): Result<Unit>
}
