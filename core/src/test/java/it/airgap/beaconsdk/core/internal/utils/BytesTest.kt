package it.airgap.beaconsdk.core.internal.utils

import org.junit.Assert.*
import org.junit.Test
import java.math.BigInteger

internal class BytesTest {
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
    fun `verifies if string is valid hex string`() {
        val allValid = validHexStrings.map(String::isHex).fold(true, Boolean::and)
        val allInvalid = invalidHexStrings.map(String::isHex).fold(false, Boolean::or)

        assertTrue(allValid)
        assertFalse(allInvalid)
    }

    @Test
    fun `parses byte as hex string`() {
        val bytes: List<Byte> = listOf(-128, -1, 0, 1, 127)
        val hexStrings: List<String> = bytes.map { it.toHexString().asString(withPrefix = true) }

        assertEquals(listOf("0x80", "0xff", "0x00", "0x01", "0x7f"), hexStrings)
    }

    @Test
    fun `parses byte array as hex string`() {
        val bytes: List<ByteArray> = listOf(
            byteArrayOf(1, 2, 3, 4, 5)
        )
        val hexStrings: List<String> = bytes.map { it.toHexString().asString(withPrefix = true) }

        assertEquals(listOf("0x0102030405"), hexStrings)
    }

    @Test
    fun `parses byte array as BigInteger`() {
        val bytes: List<ByteArray> = listOf(
            byteArrayOf(1, 2, 3, 4, 5)
        )
        val bigIntegers: List<BigInteger> = bytes.map { it.toBigInteger() }

        assertEquals(
            listOf(
                BigInteger("0102030405", 16)
            ),
            bigIntegers,
        )
    }

    @Test
    fun `parses list of bytes as hex string`() {
        val bytes: List<List<Byte>> = listOf(
            listOf(1, 2, 3, 4, 5)
        )
        val hexStrings: List<String> = bytes.map { it.toHexString().asString(withPrefix = true) }

        assertEquals(listOf("0x0102030405"), hexStrings)
    }

    @Test
    fun `creates HexString from Int`() {
        val intsWithHexStrings: List<Pair<Int, String>> = listOf(
            -2147483648 to "80000000",
            -2543 to "fffff611",
            -153 to "ffffff67",
            -1 to "ffffffff",
            0 to "00",
            127 to "7f",
            1205 to "04b5",
            2147483647 to "7fffffff",
        )

        intsWithHexStrings
            .map { it.first.toHexString() to it.second }
            .forEach {
                assertEquals(it.second, it.first.asString(withPrefix = false))
            }
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
            .map { it.first.toHexString() to it.second.asHexString() }
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
}