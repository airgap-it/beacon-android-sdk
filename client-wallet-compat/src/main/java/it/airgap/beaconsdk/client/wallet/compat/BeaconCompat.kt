package it.airgap.beaconsdk.client.wallet.compat

import kotlinx.coroutines.*

internal object BeaconCompat {
    private val _listeners: MutableMap<Int, OnNewMessageListener> = mutableMapOf()
    val listeners: Map<Int, OnNewMessageListener> get() = _listeners

    private val _scopes: MutableMap<Scope, CoroutineScope> = mutableMapOf()
    val scopes: Map<Scope, CoroutineScope> get() = _scopes

    fun addListener(listener: OnNewMessageListener): Int =
        listener.hashCode().also { _listeners[it] = listener }

    fun removeListener(listener: OnNewMessageListener) {
        _listeners.remove(listener.hashCode())
        if (_listeners.isEmpty()) {
            _scopes[Scope.Receive]?.cancel()
            _scopes.remove(Scope.Receive)
        }
    }

    fun cancelScopes() {
        with(_scopes) {
            _scopes.forEach { it.value.cancel() }
            clear()
        }

        _listeners.clear()
    }

    fun receiveScope(block: suspend () -> Unit) {
        jobScope(Scope.Receive, block)
    }

    fun sendScope(block: suspend () -> Unit) {
        jobScope(Scope.Send, block)
    }

    fun buildScope(block: suspend () -> Unit) {
        jobScope(Scope.Build, block)
    }

    fun storageScope(block: suspend () -> Unit) {
        jobScope(Scope.Storage, block)
    }

    private fun jobScope(scope: Scope, block: suspend () -> Unit) {
        _scopes
            .getActiveOrPut(scope) { CoroutineScope(CoroutineName("BeaconClient#$scope")) }
            .launch {
                block()
                _scopes.remove(scope)
            }
    }

    enum class Scope(val value: String) {
        Receive("receive"),
        Send("send"),
        Build("build"),
        Storage("storage"),
    }

    private fun MutableMap<Scope, CoroutineScope>.getActiveOrPut(key: Scope, defaultValue: () -> CoroutineScope): CoroutineScope {
        val scope = get(key)

        return if (scope == null || !scope.isActive) {
            val newScope = defaultValue()
            put(key, newScope)
            newScope
        } else {
            scope
        }
    }
}