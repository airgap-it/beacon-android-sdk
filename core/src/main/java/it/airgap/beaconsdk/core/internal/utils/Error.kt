package it.airgap.beaconsdk.core.internal.utils

import it.airgap.beaconsdk.core.exception.InternalException
import it.airgap.beaconsdk.core.message.BeaconMessage
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlin.reflect.KClass

internal fun failWith(message: String? = null, cause: Throwable? = null): Nothing =
    if (message == null && cause != null) throw cause
    else throw InternalException(message, cause)

internal fun failWithUninitialized(name: String): Nothing =
    throw IllegalStateException("$name uninitialized")

internal fun failWithIllegalState(message: String): Nothing =
    throw IllegalStateException(message)

internal fun failWithIllegalArgument(message: String): Nothing =
    throw IllegalArgumentException(message)

internal fun failWithExpectedJsonDecoder(actual: KClass<out Decoder>): Nothing =
    throw SerializationException("Expected Json decoder, got $actual")

internal fun failWithExpectedJsonEncoder(actual: KClass<out Encoder>): Nothing =
    throw SerializationException("Expected Json encoder, got $actual")

internal fun failWithUnexpectedJsonType(type: KClass<out JsonElement>): Nothing =
    throw SerializationException("Could not deserialize, unexpected JSON type $type")

internal fun failWithMissingField(name: String): Nothing =
    throw SerializationException("Could not deserialize, `$name` field is missing")

internal fun failWithUnsupportedMessage(message: BeaconMessage, version: String): Nothing =
    throw IllegalArgumentException("Message $message is not supported in version $version")
