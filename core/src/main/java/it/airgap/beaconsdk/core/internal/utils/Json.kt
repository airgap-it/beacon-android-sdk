package it.airgap.beaconsdk.core.internal.utils

import androidx.annotation.RestrictTo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import kotlin.reflect.KClass

@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun <T : Any> Json.encodeToString(value: T, sourceClass: KClass<T>): String =
    encodeToString(serializerFor(sourceClass), value)

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Suppress("UNCHECKED_CAST")
public fun <T : Any> Json.decodeFromString(string: String, targetClass: KClass<T>): T =
    decodeFromString(serializerFor(targetClass), string) as T

@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun Map<String, JsonElement>.toJsonObject(): JsonObject = JsonObject(this)

@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun JsonObject.getString(key: String): String =
    get(key)?.jsonPrimitive?.content ?: failWithMissingField(key)

@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun <T> JsonObject.getSerializable(key: String, jsonDecoder: JsonDecoder, deserializer: KSerializer<T>): T =
    get(key)?.let { jsonDecoder.json.decodeFromJsonElement(deserializer, it) } ?: failWithMissingField(key)
