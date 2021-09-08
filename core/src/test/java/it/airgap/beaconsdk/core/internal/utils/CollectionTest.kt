package it.airgap.beaconsdk.core.internal.utils

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.assertEquals

internal class CollectionTest {

    private lateinit var testDeferred: CompletableDeferred<Unit>

    @Before
    fun setup() {
        testDeferred = CompletableDeferred()
    }

    @Test
    fun `splits list into two at calculated index`() {
        assertEquals(
            Pair(emptyList(), listOf(1, 2, 3, 4, 5, 6)),
            listOf(1, 2, 3, 4, 5, 6).splitAt { 0 },
        )

        assertEquals(
            Pair(listOf(1, 2, 3, 4, 5, 6), emptyList()),
            listOf(1, 2, 3, 4, 5, 6).splitAt { it.size },
        )

        assertEquals(
            Pair(listOf(1, 2, 3), listOf(4, 5, 6)),
            listOf(1, 2, 3, 4, 5, 6).splitAt { it.size / 2 },
        )

        assertEquals(
            Pair(listOf(1, 2), listOf(3, 4, 5, 6)),
            listOf(1, 2, 3, 4, 5, 6).splitAt { it.size / 3 },
        )

        assertEquals(
            Pair(listOf(1), listOf(2, 3, 4, 5, 6)),
            listOf(1, 2, 3, 4, 5, 6).splitAt { 1 },
        )
    }

    @Test
    fun `splits list into two at calculated index and includes split index in first list`() {
        assertEquals(
            Pair(listOf(1), listOf(2, 3, 4, 5, 6)),
            listOf(1, 2, 3, 4, 5, 6).splitAt(firstInclusive = true) { 0 },
        )

        assertEquals(
            Pair(listOf(1, 2, 3, 4, 5, 6), emptyList()),
            listOf(1, 2, 3, 4, 5, 6).splitAt(firstInclusive = true) { it.size },
        )

        assertEquals(
            Pair(listOf(1, 2, 3, 4), listOf(5, 6)),
            listOf(1, 2, 3, 4, 5, 6).splitAt(firstInclusive = true) { it.size / 2 },
        )

        assertEquals(
            Pair(listOf(1, 2, 3), listOf(4, 5, 6)),
            listOf(1, 2, 3, 4, 5, 6).splitAt(firstInclusive = true) { it.size / 3 },
        )

        assertEquals(
            Pair(listOf(1, 2), listOf(3, 4, 5, 6)),
            listOf(1, 2, 3, 4, 5, 6).splitAt(firstInclusive = true) { 1 },
        )
    }

    @Test
    fun `splits list into two at specified index`() {
        assertEquals(
            Pair(emptyList(), listOf(1, 2, 3, 4, 5, 6)),
            listOf(1, 2, 3, 4, 5, 6).splitAt(0),
        )

        assertEquals(
            Pair(listOf(1, 2, 3, 4, 5, 6), emptyList()),
            listOf(1, 2, 3, 4, 5, 6).splitAt(6),
        )

        assertEquals(
            Pair(listOf(1, 2, 3), listOf(4, 5, 6)),
            listOf(1, 2, 3, 4, 5, 6).splitAt(3),
        )

        assertEquals(
            Pair(listOf(1, 2), listOf(3, 4, 5, 6)),
            listOf(1, 2, 3, 4, 5, 6).splitAt(2),
        )

        assertEquals(
            Pair(listOf(1), listOf(2, 3, 4, 5, 6)),
            listOf(1, 2, 3, 4, 5, 6).splitAt(1),
        )
    }

    @Test
    fun `splits list into two at specified index and includes split index in first list`() {
        assertEquals(
            Pair(listOf(1), listOf(2, 3, 4, 5, 6)),
            listOf(1, 2, 3, 4, 5, 6).splitAt(0, firstInclusive = true),
        )

        assertEquals(
            Pair(listOf(1, 2, 3, 4, 5, 6), emptyList()),
            listOf(1, 2, 3, 4, 5, 6).splitAt(6, firstInclusive = true),
        )

        assertEquals(
            Pair(listOf(1, 2, 3, 4), listOf(5, 6)),
            listOf(1, 2, 3, 4, 5, 6).splitAt(3, firstInclusive = true),
        )

        assertEquals(
            Pair(listOf(1, 2, 3), listOf(4, 5, 6)),
            listOf(1, 2, 3, 4, 5, 6).splitAt(2, firstInclusive = true),
        )

        assertEquals(
            Pair(listOf(1, 2), listOf(3, 4, 5, 6)),
            listOf(1, 2, 3, 4, 5, 6).splitAt(1, firstInclusive = true),
        )
    }

    @Test
    fun `returns list tail`() {
        assertEquals(
            listOf(2, 3, 4, 5, 6),
            listOf(1, 2, 3, 4, 5, 6).tail(),
        )

        assertEquals(
            emptyList(),
            listOf(1).tail(),
        )
    }

    @Test
    fun `runs suspend block for each element in list`() {
        val elements = listOf(1, 2, 3, 4, 5)

        val collected = mutableListOf<Int>()
        runBlockingTest {
            elements.launchForEach {
                delay(abs(Random.nextInt() % 1000).toLong())
                collected.add(it)
                if (collected.size == elements.size) testDeferred.complete(Unit)
            }

            testDeferred.await()

            assertEquals(collected.sorted(), elements.sorted())
        }
    }

    @Test
    fun `runs suspend block for each element in list and returns list or results`() {
        val elements = listOf(1, 2, 3, 4, 5)

        runBlockingTest {
            val results = elements.asyncMap {
                delay(abs(Random.nextInt() % 1000).toLong())
                it * 2
            }

            assertEquals(elements.map { it * 2 }, results)
        }
    }

    @Test
    fun `shifts list by offset`() {
        val elements = listOf(1, 2, 3, 4, 5)

        val shifted0 = elements.shiftedBy(0)
        val shiftedInSize = elements.shiftedBy(1)
        val shiftedExceedsSize = elements.shiftedBy(9)

        assertEquals(listOf(1, 2, 3, 4, 5), shifted0)
        assertEquals(listOf(2, 3, 4, 5, 1), shiftedInSize)
        assertEquals(listOf(5, 1, 2, 3, 4), shiftedExceedsSize)
    }

    @Test
    fun `returns and removes element from map for specified key`() {
        val map1 = mutableMapOf(
            "1" to 1,
            "2" to 2,
            "3" to 3,
        )

        assertEquals(2, map1.get("2", remove = true))
        assertEquals(mapOf("1" to 1, "3" to 3), map1.toMap())

        val map2 = mutableMapOf(
            "1" to 1,
            "2" to 2,
            "3" to 3,
        )

        assertEquals(null, map2.get("4", remove = true))
        assertEquals(mapOf("1" to 1, "2" to 2, "3" to 3), map2.toMap())

        val empty = mutableMapOf<String, Int>()

        assertEquals(null, empty.get("1", remove = true))
        assertEquals(emptyMap(), empty.toMap())
    }

    @Test
    fun `returns element from map for specified key and keeps it`() {
        val map1 = mutableMapOf(
            "1" to 1,
            "2" to 2,
            "3" to 3,
        )

        assertEquals(2, map1.get("2", remove = false))
        assertEquals(mapOf("1" to 1, "2" to 2, "3" to 3), map1.toMap())

        val map2 = mutableMapOf(
            "1" to 1,
            "2" to 2,
            "3" to 3,
        )

        assertEquals(null, map2.get("4", remove = false))
        assertEquals(mapOf("1" to 1, "2" to 2, "3" to 3), map2.toMap())

        val empty = mutableMapOf<String, Int>()

        assertEquals(null, empty.get("1", remove = false))
        assertEquals(emptyMap(), empty.toMap())
    }
}