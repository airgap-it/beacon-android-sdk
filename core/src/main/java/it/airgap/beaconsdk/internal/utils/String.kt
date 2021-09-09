package it.airgap.beaconsdk.internal.utils

internal fun String.padStartEven(padChar: Char): String {
    val nextEven = if (length % 2 == 0) length else length + 1
    return padStart(nextEven, padChar)
}

internal fun String.capitalized(): String = replaceFirstChar(Char::titlecase)