package it.airgap.beaconsdk.internal.utils

import org.junit.Test
import kotlin.test.assertEquals

internal class StringTest {

    @Test
    fun `prepends string with specified char to achieve even length`() {
        assertEquals(
            "",
            "".padStartEven('0'),
        )

        assertEquals(
            "1234",
            "1234".padStartEven('0'),
        )

        assertEquals(
            "012345",
            "12345".padStartEven('0'),
        )
    }
}