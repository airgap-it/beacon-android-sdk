package it.airgap.beaconsdk.core.internal.utils.delegate

import androidx.annotation.RestrictTo
import java.lang.ref.WeakReference

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LazyWeak<T : Any>(private val initializer: () -> T, lock: Any? = null) : Lazy<T> {
    private val lock = lock ?: this

    @Volatile private var _value: WeakReference<T> = WeakReference(null)
    override val value: T
        get() = synchronized(lock) {
            _value.get() ?: initializer().also { _value = WeakReference(it) }
        }

    override fun isInitialized(): Boolean = synchronized(lock) { _value.get() != null }

    override fun toString(): String = if (isInitialized()) value.toString() else "LazyWeak has no value reference."
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T : Any> lazyWeak(lock: Any? = null, initializer: () -> T): LazyWeak<T> = LazyWeak(initializer, lock)