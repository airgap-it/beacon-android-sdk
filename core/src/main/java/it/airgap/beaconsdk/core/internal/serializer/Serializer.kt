package it.airgap.beaconsdk.core.internal.serializer

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.serializer.provider.SerializerProvider
import kotlin.reflect.KClass

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Serializer(private val serializerProvider: SerializerProvider) {
    public inline fun <reified T : Any> serialize(message: T): Result<String> =
        serialize(message, T::class)

    public fun <T : Any> serialize(message: T, sourceClass: KClass<T>): Result<String> =
        runCatching { serializerProvider.serialize(message, sourceClass) }

    public inline fun <reified T : Any> deserialize(message: String): Result<T> =
        deserialize(message, T::class)

    public fun <T : Any> deserialize(message: String, targetClass: KClass<T>): Result<T> =
        runCatching { serializerProvider.deserialize(message, targetClass) }
}