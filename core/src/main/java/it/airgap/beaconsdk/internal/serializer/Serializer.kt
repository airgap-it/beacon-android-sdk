package it.airgap.beaconsdk.internal.serializer

import it.airgap.beaconsdk.internal.serializer.provider.SerializerProvider
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.tryResult

internal class Serializer(private val serializerProvider: SerializerProvider) {
    inline fun <reified T : Any> serialize(message: T): InternalResult<String> =
        tryResult { serializerProvider.serialize(message, T::class) }

    inline fun <reified T : Any> deserialize(message: String): InternalResult<T> =
        tryResult { serializerProvider.deserialize(message, T::class) }
}