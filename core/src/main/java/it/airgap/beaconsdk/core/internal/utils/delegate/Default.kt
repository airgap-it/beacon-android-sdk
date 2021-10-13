package it.airgap.beaconsdk.core.internal.utils.delegate

import androidx.annotation.RestrictTo
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Default<V : Any>(initValue: V? = null, private val defaultValue: () -> V) : ReadWriteProperty<Any?, V> {
    private var value: V? = initValue

    override fun getValue(thisRef: Any?, property: KProperty<*>): V = value ?: defaultValue()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        this.value = value
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <V : Any> default(initValue: V? = null, defaultValue: () -> V): Default<V> = Default(initValue, defaultValue)