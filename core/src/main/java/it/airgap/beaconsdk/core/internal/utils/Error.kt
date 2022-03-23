package it.airgap.beaconsdk.core.internal.utils

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.exception.BlockchainNotFoundException
import it.airgap.beaconsdk.core.exception.InternalException
import it.airgap.beaconsdk.core.message.BeaconMessage
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlin.reflect.KClass

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun failWith(message: String? = null, cause: Throwable? = null): Nothing =
    if (message == null && cause != null) throw cause
    else throw InternalException(message, cause)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun failWithUninitialized(name: String): Nothing =
    throw IllegalStateException("$name uninitialized")

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun failWithIllegalState(message: String? = null): Nothing =
    throw IllegalStateException(message)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun failWithIllegalArgument(message: String? = null): Nothing =
    throw IllegalArgumentException(message)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun failWithBlockchainNotFound(identifier: String): Nothing =
    throw BlockchainNotFoundException(identifier)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun failWithExpectedJsonDecoder(actual: KClass<out Decoder>): Nothing =
    throw SerializationException("Expected Json decoder, got $actual")

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun failWithExpectedJsonEncoder(actual: KClass<out Encoder>): Nothing =
    throw SerializationException("Expected Json encoder, got $actual")

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun failWithUnexpectedJsonType(type: KClass<out JsonElement>): Nothing =
    throw SerializationException("Could not deserialize, unexpected JSON type $type")

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun failWithMissingField(name: String): Nothing =
    throw SerializationException("Could not deserialize, `$name` field is missing")

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun failWithUnsupportedMessage(message: BeaconMessage, version: String): Nothing =
    throw IllegalArgumentException("Message $message is not supported in version $version")

public fun failWithUnsupportedMessageVersion(version: String, blockchainIdentifier: String): Nothing =
    throw IllegalArgumentException("Message version $version is not supported for $blockchainIdentifier")
