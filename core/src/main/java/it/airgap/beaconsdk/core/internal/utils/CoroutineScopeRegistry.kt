package it.airgap.beaconsdk.core.internal.utils

import androidx.annotation.RestrictTo
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CoroutineScopeRegistry(
    private val name: String,
    private val context: CoroutineContext = Dispatchers.Default,
) {
    private val mutex: Mutex = Mutex()
    private val scopes: MutableMap<String, CoroutineScope> = mutableMapOf()

    public suspend fun get(id: String): CoroutineScope = mutex.withLock {
        scopes.getIfActiveOrPut(id) { DisposableCoroutineScope(id) }
    }

    public suspend fun cancel(id: String) {
        mutex.withLock {
            scopes[id]?.cancel(message = "Scope ${scopeName(id)} canceled.")
            scopes.remove(id)
        }
    }

    public suspend fun cancelAll() {
        mutex.withLock {
            scopes.forEach { it.value.cancel(message = "Scope ${scopeName(it.key)} canceled.") }
            scopes.clear()
        }
    }

    public suspend fun isEmpty(): Boolean = mutex.withLock { scopes.isEmpty() }

    private fun scopeName(id: String): String = "$name@$id"

    @Suppress("FunctionName")
    private fun DisposableCoroutineScope(id: String): CoroutineScope =
        CoroutineScope(CoroutineName(scopeName(id)) + context).also { it.removeOnCompletion(id) }

    private fun CoroutineScope.removeOnCompletion(id: String) {
        coroutineContext.job.invokeOnCompletion {
            CoroutineScope(Dispatchers.Default).launch {
                mutex.withLock { scopes.remove(id) }
            }
        }
    }

    private fun MutableMap<String, CoroutineScope>.getIfActiveOrPut(key: String, defaultValue: () -> CoroutineScope): CoroutineScope =
        get(key)?.takeIf { it.isActive } ?: defaultValue().also { put(key, it) }
}