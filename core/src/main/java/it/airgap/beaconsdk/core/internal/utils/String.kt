package it.airgap.beaconsdk.core.internal.utils

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun String.padStartEven(padChar: Char): String {
    val nextEven = if (length % 2 == 0) length else length + 1
    return padStart(nextEven, padChar)
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun String.capitalized(): String = replaceFirstChar(Char::titlecase)