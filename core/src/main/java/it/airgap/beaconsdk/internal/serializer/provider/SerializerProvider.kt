package it.airgap.beaconsdk.internal.serializer.provider

import kotlin.reflect.KClass

internal interface SerializerProvider {
    @Throws(Exception::class)
    fun <T : Any> serialize(message: T, sourceClass: KClass<T>): String

    @Throws(Exception::class)
    fun <T : Any> deserialize(message: String, targetClass: KClass<T>): T
}