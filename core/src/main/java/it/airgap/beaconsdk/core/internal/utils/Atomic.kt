package it.airgap.beaconsdk.core.internal.utils

import androidx.annotation.RestrictTo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Atomic<A> {

    @PublishedApi
    internal val locks: MutableMap<A, Mutex> = mutableMapOf()

    public suspend inline fun <T> action(id: A, action: () -> T): T =
        locks.getOrPut(id) { Mutex() }.withLock { action() }
}