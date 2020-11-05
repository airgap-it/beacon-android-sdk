package it.airgap.beaconsdk.internal.utils

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal inline fun <T> List<T>.splitAt(
    firstInclusive: Boolean = false,
    calculateIndex: (List<T>) -> Int,
): Pair<List<T>, List<T>> {
    val index = calculateIndex(this)
    return splitAt(index, firstInclusive)
}

internal fun <T> List<T>.splitAt(
    index: Int,
    firstInclusive: Boolean = false,
): Pair<List<T>, List<T>> {
    val splitIndex = if (firstInclusive) minOf(index + 1, size) else index

    val first = slice(0 until splitIndex)
    val second = slice(splitIndex until size)

    return Pair(first, second)
}

internal fun <T> List<T>.tail(): List<T> {
    val n = if (size > 0) size - 1 else 0
    return takeLast(n)
}

internal suspend fun <T> List<T>.launch(block: suspend (T) -> Unit) {
    coroutineScope {
        forEach {
            launch { block(it) }
        }
    }
}

internal fun <T> MutableList<T>.pop(predicate: (T) -> Boolean): T? =
    find(predicate)?.also { remove(it) }