package it.airgap.beaconsdk.core.internal.utils

import androidx.annotation.RestrictTo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import kotlin.reflect.KClass

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T : Any> Json.encodeToString(value: T, sourceClass: KClass<T>): String =
    encodeToString(serializerFor(sourceClass), value)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("UNCHECKED_CAST")
public fun <T : Any> Json.decodeFromString(string: String, targetClass: KClass<T>): T =
    decodeFromString(serializerFor(targetClass), string)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun JsonObject.getString(key: String): String =
    get(key)?.jsonPrimitive?.content ?: failWithMissingField(key)

public fun JsonObject.getStringOrNull(key: String): String? =
    get(key)?.jsonPrimitive?.content

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> JsonObject.getSerializable(key: String, jsonDecoder: JsonDecoder, deserializer: KSerializer<T>): T =
    get(key)?.let { jsonDecoder.json.decodeFromJsonElement(deserializer, it) } ?: failWithMissingField(key)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> JsonObject.getSerializableOrNull(key: String, jsonDecoder: JsonDecoder, deserializer: KSerializer<T>): T? =
    get(key)?.let {
        when (it) {
            is JsonNull -> null
            else -> jsonDecoder.json.decodeFromJsonElement(deserializer, it)
        }
    }