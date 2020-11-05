package it.airgap.beaconsdk.internal.utils

import it.airgap.beaconsdk.exception.BeaconException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlin.reflect.KClass

internal fun failWith(message: String? = null, cause: Throwable? = null): Nothing =
    if (message == null && cause != null) throw cause
    else throw BeaconException.Internal(message, cause)

internal fun failWithUninitialized(name: String): Nothing =
    throw IllegalStateException("$name uninitialized")

internal fun failWithExpectedJsonDecoder(actual: KClass<out Decoder>): Nothing =
    throw SerializationException("Expected Json decoder, got $actual")

internal fun failWithMissingField(name: String): Nothing =
    throw SerializationException("Could not deserialize, `$name` field is missing")
