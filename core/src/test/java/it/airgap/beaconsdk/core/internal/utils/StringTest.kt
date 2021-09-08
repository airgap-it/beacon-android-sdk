package it.airgap.beaconsdk.core.internal.utils

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

    @Test
    fun `capitalizes string`() {
        assertEquals(
            "String",
            "string".capitalized(),
        )

        assertEquals(
            "String",
            "String".capitalized(),
        )

        assertEquals(
            "STRING",
            "sTRING".capitalized(),
        )
    }
}