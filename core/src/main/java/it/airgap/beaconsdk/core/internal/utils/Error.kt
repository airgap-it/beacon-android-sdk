package it.airgap.beaconsdk.core.internal.utils

import it.airgap.beaconsdk.core.exception.InternalException
import it.airgap.beaconsdk.core.message.BeaconMessage
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlin.reflect.KClass

public fun failWith(message: String? = null, cause: Throwable? = null): Nothing =
    if (message == null && cause != null) throw cause
    else throw InternalException(message, cause)

public fun failWithUninitialized(name: String): Nothing =
    throw IllegalStateException("$name uninitialized")

public fun failWithIllegalState(message: String? = null): Nothing =
    throw IllegalStateException(message)

public fun failWithIllegalArgument(message: String? = null): Nothing =
    throw IllegalArgumentException(message)

public fun failWithChainNotFound(identifier: String): Nothing =
    throw IllegalStateException("Chain \"$identifier\" could not be found")

public fun failWithExpectedJsonDecoder(actual: KClass<out Decoder>): Nothing =
    throw SerializationException("Expected Json decoder, got $actual")

public fun failWithExpectedJsonEncoder(actual: KClass<out Encoder>): Nothing =
    throw SerializationException("Expected Json encoder, got $actual")

public fun failWithUnexpectedJsonType(type: KClass<out JsonElement>): Nothing =
    throw SerializationException("Could not deserialize, unexpected JSON type $type")

public fun failWithMissingField(name: String): Nothing =
    throw SerializationException("Could not deserialize, `$name` field is missing")

public fun failWithUnsupportedMessage(message: BeaconMessage, version: String): Nothing =
    throw IllegalArgumentException("Message $message is not supported in version $version")
