package it.airgap.beaconsdk.internal.serializer.provider

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

internal class MockSerializerProvider : SerializerProvider {
    private val json: Json by lazy { Json { classDiscriminator = "_type" } }
    override fun <T : Any> serialize(message: T, sourceClass: KClass<T>): String =
        json.encodeToString(serializerFor(sourceClass), message)

    override fun <T : Any> deserialize(message: String, targetClass: KClass<T>): T =
        json.decodeFromString(serializerFor(targetClass), message) as T

    private fun serializerFor(target: KClass<out Any>): KSerializer<Any?> {
        val type = target.createType()
        return serializer(type)
    }
}