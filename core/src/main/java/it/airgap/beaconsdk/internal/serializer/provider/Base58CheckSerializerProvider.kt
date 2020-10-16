package it.airgap.beaconsdk.internal.serializer.provider

import it.airgap.beaconsdk.internal.utils.Base58Check
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

internal class Base58CheckSerializerProvider(private val base58Check: Base58Check) : SerializerProvider {
    private val json: Json by lazy { Json { classDiscriminator = "_type" } }

    override fun <T : Any> serialize(message: T, sourceClass: KClass<T>): String {
        val jsonEncoded = json.encodeToString(serializerFor(sourceClass), message)
        return base58Check.encode(jsonEncoded.toByteArray()).getOrThrow()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> deserialize(message: String, targetClass: KClass<T>): T {
        val decoded = base58Check.decode(message).getOrThrow()
        val jsonEncoded = String(decoded)

        return json.decodeFromString(serializerFor(targetClass), jsonEncoded) as T
    }

    private fun serializerFor(target: KClass<out Any>): KSerializer<Any?> {
        val type = target.createType()
        return serializer(type)
    }
}