package it.airgap.beaconsdk.core.internal.utils

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun ByteArray.trimLeadingZeros(): ByteArray {
    val startIndex = indexOfFirst { it != (0).toByte() }.takeIf { it >= 0 } ?: lastIndex
    return sliceArray(startIndex..lastIndex)
}

public fun ByteArray.splitAt(
    firstInclusive: Boolean = false,
    calculateIndex: (ByteArray) -> Int,
): Pair<ByteArray, ByteArray> {
    val index = calculateIndex(this)
    return splitAt(index, firstInclusive)
}

public fun ByteArray.splitAt(
    index: Int,
    firstInclusive: Boolean = false,
): Pair<ByteArray, ByteArray> {
    val splitIndex = if (firstInclusive) minOf(index + 1, size) else index

    val first = sliceArray(0 until splitIndex)
    val second = sliceArray(splitIndex until size)

    return Pair(first, second)
}