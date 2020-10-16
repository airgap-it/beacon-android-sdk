package it.airgap.beaconsdk.internal.serializer

import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.serializer.provider.SerializerProvider
import it.airgap.beaconsdk.internal.utils.tryInternal

internal class Serializer(private val serializerProvider: SerializerProvider) {
    inline fun <reified T : Any> serialize(message: T): InternalResult<String> =
        tryInternal { serializerProvider.serialize(message, T::class) }

    inline fun <reified T : Any> deserialize(message: String): InternalResult<T> =
        tryInternal { serializerProvider.deserialize(message, T::class) }
}