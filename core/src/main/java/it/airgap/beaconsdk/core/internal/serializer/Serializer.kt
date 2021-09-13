package it.airgap.beaconsdk.core.internal.serializer

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.serializer.provider.SerializerProvider

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Serializer(public val serializerProvider: SerializerProvider) {
    public inline fun <reified T : Any> serialize(message: T): Result<String> =
        runCatching { serializerProvider.serialize(message, T::class) }

    public inline fun <reified T : Any> deserialize(message: String): Result<T> =
        runCatching { serializerProvider.deserialize(message, T::class) }
}