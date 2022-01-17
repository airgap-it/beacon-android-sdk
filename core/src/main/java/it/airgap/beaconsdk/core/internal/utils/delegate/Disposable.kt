package it.airgap.beaconsdk.core.internal.utils.delegate

import androidx.annotation.RestrictTo
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Disposable<V : Any> : ReadWriteProperty<Any?, V?> {
    private var value: V? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): V? =
        value?.also { value = null }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V?) {
        this.value = value
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T: Any> disposable(): Disposable<T> = Disposable()