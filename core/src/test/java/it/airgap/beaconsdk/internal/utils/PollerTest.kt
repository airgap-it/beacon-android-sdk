package it.airgap.beaconsdk.internal.utils

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class PollerTest {

    private lateinit var poller: Poller

    @Before
    fun setup() {
        poller = Poller()
    }

    @Test
    fun `executes specified action periodically`() {
        val expected = listOf(1, 2, 3, 4, 5)
        runBlocking {
            val iterator = expected.iterator()
            val actual = async {
                poller.poll {
                    if (iterator.hasNext()) Success(iterator.next())
                    else Failure()
                }.mapNotNull { it.getOrNull() }
                    .take(expected.size)
                    .toList()
            }

            assertEquals(expected, actual.await())
        }
    }
}