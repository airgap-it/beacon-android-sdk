package it.airgap.beaconsdk.internal.serializer.provider

import it.airgap.beaconsdk.internal.utils.decodeFromString
import it.airgap.beaconsdk.internal.utils.encodeToString
import it.airgap.beaconsdk.internal.utils.failWith
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

internal class MockSerializerProvider(var shouldFail: Boolean = false) : SerializerProvider {
    private val json: Json by lazy { Json { classDiscriminator = "_type" } }

    @Throws(Exception::class)
    override fun <T : Any> serialize(message: T, sourceClass: KClass<T>): String =
        if (shouldFail) failWith()
        else json.encodeToString(message, sourceClass)

    override fun <T : Any> deserialize(message: String, targetClass: KClass<T>): T =
        if (shouldFail) failWith()
        else json.decodeFromString(message, targetClass)
}