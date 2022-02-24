package it.airgap.beaconsdk.core.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = Maybe.Serializer::class)
public sealed class Maybe<out T> {

    @Serializable
    public data class Some<T>(public val value: T) : Maybe<T>()

    public object None : Maybe<Any>()

    public data class NoneWithError<T>(public val exception: Throwable) : Maybe<T>()

    internal class Serializer<T>(private val valueSerializer: KSerializer<T>) : KSerializer<Maybe<T>> {
        override val descriptor: SerialDescriptor = valueSerializer.descriptor

        override fun deserialize(decoder: Decoder): Maybe<T> =
            try {
                val value = decoder.decodeSerializableValue(valueSerializer)
                Some(value)
            } catch (e: Exception) {
                NoneWithError(e)
            }

        override fun serialize(encoder: Encoder, value: Maybe<T>) {
            when (value) {
                is Some -> encoder.encodeSerializableValue(valueSerializer, value.value)
                is None -> { /* no action */ }
                is NoneWithError -> { /* no action */ }
            }
        }
    }
}