package it.airgap.beaconsdk.core.internal.serializer.provider

import androidx.annotation.RestrictTo
import kotlin.reflect.KClass

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface SerializerProvider {
    @Throws(Exception::class)
    public fun <T : Any> serialize(message: T, sourceClass: KClass<T>): String

    @Throws(Exception::class)
    public fun <T : Any> deserialize(message: String, targetClass: KClass<T>): T
}