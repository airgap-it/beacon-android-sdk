package it.airgap.beaconsdk.core.internal.utils

import androidx.annotation.RestrictTo
import io.ktor.util.reflect.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class KJsonSerializer<T> : KSerializer<T> {
    override fun deserialize(decoder: Decoder): T {
        val jsonDecoder = decoder as? JsonDecoder ?: failWithExpectedJsonDecoder(decoder::class)
        val jsonElement = jsonDecoder.decodeJsonElement()

        return deserialize(jsonDecoder, jsonElement)
    }

    public abstract fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): T

    override fun serialize(encoder: Encoder, value: T) {
        val jsonEncoder = encoder as? JsonEncoder ?: failWithExpectedJsonEncoder(encoder::class)

        serialize(jsonEncoder, value)
    }

    public abstract fun serialize(jsonEncoder: JsonEncoder, value: T)
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PolymorphicSerializer<P : Any, S : P>(private val subclass: KClass<S>, private val subclassSerializer: KSerializer<S>): KSerializer<P> {
    override val descriptor: SerialDescriptor get() = subclassSerializer.descriptor
    override fun deserialize(decoder: Decoder): P = subclassSerializer.deserialize(decoder)
    override fun serialize(encoder: Encoder, value: P) {
        if (value.instanceOf(subclass)) encoder.encodeSerializableValue(subclassSerializer, value as S)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
public inline fun <P : Any, reified S : P> PolymorphicSerializer(subclassSerializer: KSerializer<S>): PolymorphicSerializer<P, S> =
    PolymorphicSerializer(S::class, subclassSerializer)

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> serializerFor(target: KClass<out T>): KSerializer<T> {
    val type = target.createType()
    return serializer(type) as KSerializer<T>
}