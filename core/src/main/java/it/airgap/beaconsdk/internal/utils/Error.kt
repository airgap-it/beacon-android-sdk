package it.airgap.beaconsdk.internal.utils

import it.airgap.beaconsdk.exception.BeaconException

internal fun failWith(message: String? = null, cause: Throwable? = null): Nothing =
    if (message == null && cause != null) throw cause
    else throw BeaconException.Unknown(message, cause)

internal fun failWithUninitialized(name: String): Nothing = throw IllegalStateException(uninitializedMessage(name))

internal fun uninitializedMessage(name: String): String = "$name uninitialized, call `init` first"

