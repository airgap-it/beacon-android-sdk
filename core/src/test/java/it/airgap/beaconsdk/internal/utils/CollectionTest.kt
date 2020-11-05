package it.airgap.beaconsdk.internal.utils

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
    fun `runs suspend block for with each element in list`() {
        val elements = listOf(1, 2, 3, 4, 5)

        val collected = mutableListOf<Int>()
        runBlockingTest {
            elements.launch {
                delay(abs(Random.nextInt() % 1000).toLong())
                collected.add(it)
                if (collected.size == elements.size) testDeferred.complete(Unit)
            }

            testDeferred.await()

            assertEquals(collected.sorted(), elements.sorted())
        }
    }

    @Test
    fun `finds and removes first element from list based on predicate`() {
        val list1 = mutableListOf(1, 2, 3, 4, 5)

        assertEquals(2, list1.pop { it % 2 == 0 })
        assertEquals(listOf(1, 3, 4, 5), list1.toList())

        val list2 = mutableListOf(1, 2, 3, 4, 5)

        assertEquals(null, list2.pop { it < 0 })
        assertEquals(listOf(1, 2, 3, 4, 5), list2.toList())

        val empty = mutableListOf<Int>()

        assertEquals(null, empty.pop { it % 2 == 0 })
        assertEquals(emptyList(), empty.toList())
    }
}