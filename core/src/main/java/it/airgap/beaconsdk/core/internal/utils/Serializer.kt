package it.airgap.beaconsdk.core.internal.utils

import androidx.annotation.RestrictTo
import io.ktor.util.reflect.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.modules.serializersModuleOf
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface KJsonSerializer<T> : KSerializer<T> {
    override fun deserialize(decoder: Decoder): T {
        val jsonDecoder = decoder as? JsonDecoder ?: failWithExpectedJsonDecoder(decoder::class)
        val jsonElement = jsonDecoder.decodeJsonElement()

        return deserialize(jsonDecoder, jsonElement)
    }

    public fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): T

    override fun serialize(encoder: Encoder, value: T) {
        val jsonEncoder = encoder as? JsonEncoder ?: failWithExpectedJsonEncoder(encoder::class)

        serialize(jsonEncoder, value)
    }

    public fun serialize(jsonEncoder: JsonEncoder, value: T)
}

@Suppress("UNCHECKED_CAST")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SuperClassSerializer<T : Any, S : T>(private val subClass: KClass<S>, private val subClassSerializer: KSerializer<S>): KSerializer<T> {
    override val descriptor: SerialDescriptor get() = subClassSerializer.descriptor
    override fun deserialize(decoder: Decoder): T = subClassSerializer.deserialize(decoder)
    override fun serialize(encoder: Encoder, value: T) {
        if (value.instanceOf(subClass)) encoder.encodeSerializableValue(subClassSerializer, value as S)
        else failWithIllegalArgument("Unexpected value of type ${value::class}")
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("FunctionName")
public inline fun <T : Any, reified S : T> SuperClassSerializer(subClassSerializer: KSerializer<S>): SuperClassSerializer<T, S> =
    SuperClassSerializer(S::class, subClassSerializer)

@OptIn(ExperimentalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> Json.serializerFor(target: KClass<out T>): KSerializer<T> {
    val type = target.createType()
    return (serializersModule.getContextual(target) ?: serializersModule.serializer(type)) as KSerializer<T>
}