package it.airgap.beaconsdk.internal.serializer.provider

import it.airgap.beaconsdk.internal.utils.Base58Check
import it.airgap.beaconsdk.internal.utils.decodeFromString
import it.airgap.beaconsdk.internal.utils.encodeToString
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

internal class Base58CheckSerializerProvider(private val base58Check: Base58Check) : SerializerProvider {
    private val json: Json by lazy { Json { ignoreUnknownKeys = true } }

    override fun <T : Any> serialize(message: T, sourceClass: KClass<T>): String {
        val jsonEncoded = json.encodeToString(message, sourceClass)
        return base58Check.encode(jsonEncoded.encodeToByteArray()).get()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> deserialize(message: String, targetClass: KClass<T>): T {
        val decoded = base58Check.decode(message).get()
        val jsonEncoded = decoded.decodeToString()

        return json.decodeFromString(jsonEncoded, targetClass)
    }
}