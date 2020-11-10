package it.airgap.beaconsdk.internal.utils

import org.junit.Assert.*
import org.junit.Test
import java.math.BigInteger
import kotlin.test.assertFailsWith

internal class HexTest {

    private val hexPrefix: String = "0x"
    private val validHexStrings: List<String> = listOf(
        "9434dc98",
        "0x7b1ea2cb",
        "e40476d7",
        "c47320abdd31",
        "0x5786dac9eaf4",
    )

    private val invalidHexStrings: List<String> = listOf(
        "",
        "9434dc98az",
        "0xe40476d77t",
        "0x",
        "0x1",
    )

    @Test
    fun `creates HexString from valid string`() {
        val hexStrings = validHexStrings.map(HexString.Companion::fromString)

        assertEquals(validHexStrings.map(this::withoutHexPrefix), hexStrings.map(HexString::value))
    }

    @Test
    fun `returns null when creating HexString from invalid string and asked to`() {
        val hexString = invalidHexStrings.mapNotNull(HexString.Companion::fromStringOrNull)

        assertEquals(0, hexString.size)
    }

    @Test
    fun `fails when creating HexString form invalid string`() {
        invalidHexStrings.forEach {
            assertFailsWith<IllegalArgumentException> {
                HexString.fromString(it)
            }
        }
    }

    @Test
    fun `returns HexString length with and without prefix`() {
        val string = validHexStrings.first()
        val hexString = HexString.fromString(string)

        assertEquals(withHexPrefix(string).length, hexString.length(withPrefix = true))
        assertEquals(withoutHexPrefix(string).length, hexString.length(withPrefix = false))
    }

    @Test
    fun `returns HexString value with and without prefix`() {
        val string = validHexStrings.first()
        val hexString = HexString.fromString(string)

        assertEquals(withHexPrefix(string), hexString.value(withPrefix = true))
        assertEquals(withoutHexPrefix(string), hexString.value())
    }

    @Test
    fun `slices HexString`() {
        val string = validHexStrings.first()
        val hexString = HexString.fromString(string)

        val sliceRange = 0 until (string.length / 2)
        val sliceIndex = string.length / 2

        val sliceWithRange = hexString.slice(sliceRange)
        val sliceWithIndex = hexString.slice(sliceIndex)

        assertEquals(withoutHexPrefix(string).slice(sliceRange), sliceWithRange.value())
        assertEquals(withoutHexPrefix(string).substring(sliceIndex), sliceWithIndex.value())
    }

    @Test
    fun `creates byte array from HexString`() {
        val hexStringsWithExpected: List<Pair<String, ByteArray>> = listOf(
            "9434dc98" to byteArrayOf(-108, 52, -36, -104),
            "0x7b1ea2cb" to byteArrayOf(123, 30, -94, -53),
            "e40476d7" to byteArrayOf(-28, 4, 118, -41),
            "c47320abdd31" to byteArrayOf(-60, 115, 32, -85, -35, 49),
            "0x5786dac9eaf4" to byteArrayOf(87, -122, -38, -55, -22, -12),
        )

        hexStringsWithExpected
            .map { HexString.fromString(it.first).asByteArray() to it.second }
            .forEach {
                assertArrayEquals(it.second, it.first)
            }
    }

    @Test
    fun `creates BigInteger from HexString`() {
        val hexStringsWithExpected: List<Pair<String, BigInteger>> =
            validHexStrings.map { it to BigInteger(withoutHexPrefix(it), 16) }

        hexStringsWithExpected
            .map { HexString.fromString(it.first).toBigInteger() to it.second }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `verifies if string is valid hex string`() {
        val allValid = validHexStrings.map(String::isHex).fold(true, Boolean::and)
        val allInvalid = invalidHexStrings.map(String::isHex).fold(false, Boolean::or)

        assertTrue(allValid)
        assertFalse(allInvalid)
    }

    @Test
    fun `parses byte as hex string`() {
        val bytes: List<Byte> = listOf(-128, -1, 0, 1, 127)
        val hexStrings: List<String> = bytes.map { it.asHexString().value(withPrefix = true) }

        assertEquals(listOf("0x80", "0xff", "0x00", "0x01", "0x7f"), hexStrings)
    }

    @Test
    fun `parses byte array as hex string`() {
        val bytes: List<ByteArray> = listOf(
            byteArrayOf(1, 2, 3, 4, 5)
        )
        val hexStrings: List<String> = bytes.map { it.asHexString().value(withPrefix = true) }

        assertEquals(listOf("0x0102030405"), hexStrings)
    }

    @Test
    fun `parses list of bytes as hex string`() {
        val bytes: List<List<Byte>> = listOf(
            listOf(1, 2, 3, 4, 5)
        )
        val hexStrings: List<String> = bytes.map { it.asHexString().value(withPrefix = true) }

        assertEquals(listOf("0x0102030405"), hexStrings)
    }

    @Test
    fun `creates HexString from Int`() {
        val intsWithHexStrings: List<Pair<Int, String>> = listOf(
            0,
            127,
            1205,
            Int.MAX_VALUE,
        ).map { it to it.toString(16).padStartEven('0') }

        intsWithHexStrings
            .map { it.first.asHexString() to HexString.fromString(it.second) }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `fails to create HexString from negative Int`() {
        val negativeInts: List<Int> = listOf(
            Int.MIN_VALUE,
            -4124,
            -643,
            -74,
            -1,
        )

        negativeInts.forEach {
            assertFailsWith<IllegalArgumentException> {
                it.asHexString()
            }
        }
    }

    @Test
    fun `returns null when creating HexString from negative Int and asked to`() {
        val negativeInts: List<Int> = listOf(
            Int.MIN_VALUE,
            -4124,
            -643,
            -74,
            -1,
        )

        val hexStrings = negativeInts.mapNotNull(Int::asHexStringOrNull)

        assertEquals(0, hexStrings.size)
    }

    @Test
    fun `creates HexString from BigInteger`() {
        val bigIntegersWithExpected: List<Pair<BigInteger, String>> = listOf(
            BigInteger.ZERO,
            BigInteger.ONE,
            BigInteger.TEN,
            BigInteger.valueOf(124),
            BigInteger.valueOf(6346),
            BigInteger.valueOf(Long.MAX_VALUE),
        ).map { it to it.toString(16).padStartEven('0') }

        bigIntegersWithExpected
            .map { it.first.asHexString() to HexString.fromString(it.second) }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `returns null when creating HexString from negative BigInteger and asked to`() {
        val negativeBigIntegers: List<BigInteger> = listOf(
            Long.MIN_VALUE,
            -5236764366,
            -346657,
            -5265,
            -1,
        ).map(BigInteger::valueOf)

        val hexStrings = negativeBigIntegers.mapNotNull(BigInteger::asHexStringOrNull)

        assertEquals(0, hexStrings.size)
    }

    private fun withHexPrefix(string: String): String = if (string.startsWith(hexPrefix)) string else "${hexPrefix}${string}"
    private fun withoutHexPrefix(string: String): String = if (string.startsWith(hexPrefix)) string.substring(hexPrefix.length) else string

}