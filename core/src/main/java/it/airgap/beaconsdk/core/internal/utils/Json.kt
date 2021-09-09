package it.airgap.beaconsdk.core.internal.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

public fun <T : Any> Json.encodeToString(value: T, sourceClass: KClass<T>): String =
    encodeToString(serializerFor(sourceClass), value)

@Suppress("UNCHECKED_CAST")
public fun <T : Any> Json.decodeFromString(string: String, targetClass: KClass<T>): T =
    decodeFromString(serializerFor(targetClass), string) as T

public fun Map<String, JsonElement>.toJsonObject(): JsonObject = JsonObject(this)

private fun serializerFor(target: KClass<out Any>): KSerializer<Any?> {
    val type = target.createType()
    return serializer(type)
}