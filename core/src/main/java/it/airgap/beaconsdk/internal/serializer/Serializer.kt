package it.airgap.beaconsdk.internal.serializer

import it.airgap.beaconsdk.internal.serializer.provider.SerializerProvider

internal class Serializer(private val serializerProvider: SerializerProvider) {
    inline fun <reified T : Any> serialize(message: T): Result<String> =
        runCatching { serializerProvider.serialize(message, T::class) }

    inline fun <reified T : Any> deserialize(message: String): Result<T> =
        runCatching { serializerProvider.deserialize(message, T::class) }
}