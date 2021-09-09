package it.airgap.beaconsdk.core.internal.utils

public fun String.padStartEven(padChar: Char): String {
    val nextEven = if (length % 2 == 0) length else length + 1
    return padStart(nextEven, padChar)
}

public fun String.capitalized(): String = replaceFirstChar(Char::titlecase)