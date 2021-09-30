package it.airgap.beaconsdk.core.internal.data

import it.airgap.beaconsdk.core.internal.utils.asHexString
import it.airgap.beaconsdk.core.internal.utils.asHexStringOrNull
import org.junit.Assert
import org.junit.Test
import java.math.BigInteger
import kotlin.test.assertFailsWith

internal class HexStringTest {
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
        val hexStrings = validHexStrings.map(String::asHexString)

        Assert.assertEquals(validHexStrings.map(this::withoutHexPrefix),
            hexStrings.map(HexString::asString))
    }

    @Test
    fun `returns null when creating HexString from invalid string and asked to`() {
        val hexString = invalidHexStrings.mapNotNull(String::asHexStringOrNull)

        Assert.assertEquals(0, hexString.size)
    }

    @Test
    fun `fails when creating HexString form invalid string`() {
        invalidHexStrings.forEach {
            assertFailsWith<IllegalArgumentException> {
                it.asHexString()
            }
        }
    }

    @Test
    fun `returns HexString length with and without prefix`() {
        val string = validHexStrings.first()
        val hexString = string.asHexString()

        Assert.assertEquals(withHexPrefix(string).length, hexString.length(withPrefix = true))
        Assert.assertEquals(withoutHexPrefix(string).length, hexString.length(withPrefix = false))
    }

    @Test
    fun `returns HexString value with and without prefix`() {
        val string = validHexStrings.first()
        val hexString = string.asHexString()

        Assert.assertEquals(withHexPrefix(string), hexString.asString(withPrefix = true))
        Assert.assertEquals(withoutHexPrefix(string), hexString.asString())
    }

    @Test
    fun `slices HexString`() {
        val string = validHexStrings.first()
        val hexString = string.asHexString()

        val sliceRange = 0 until (string.length / 2)
        val sliceIndex = string.length / 2

        val sliceWithRange = hexString.slice(sliceRange)
        val sliceWithIndex = hexString.slice(sliceIndex)

        Assert.assertEquals(withoutHexPrefix(string).slice(sliceRange), sliceWithRange.asString())
        Assert.assertEquals(withoutHexPrefix(string).substring(sliceIndex),
            sliceWithIndex.asString())
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
            .map { it.first.asHexString().toByteArray() to it.second }
            .forEach {
                Assert.assertArrayEquals(it.second, it.first)
            }
    }

    @Test
    fun `creates Int from HexString`() {
        val hexStringsWithExpected: List<Pair<String, Int>> = listOf(
            "80000000" to -2147483648,
            "fffff611" to -2543,
            "ffffff67" to -153,
            "ffffffff" to -1,
            "00" to 0,
            "7f" to 127,
            "04b5" to 1205,
            "7fffffff" to 2147483647,
        )

        hexStringsWithExpected
            .map { it.first.asHexString().toInt() to it.second }
            .forEach {
                Assert.assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `creates BigInteger from HexString`() {
        val hexStringsWithExpected: List<Pair<String, BigInteger>> =
            validHexStrings.map { it to BigInteger(withoutHexPrefix(it), 16) }

        hexStringsWithExpected
            .map { it.first.asHexString().toBigInteger() to it.second }
            .forEach {
                Assert.assertEquals(it.second, it.first)
            }
    }

    private fun withHexPrefix(string: String): String = if (string.startsWith(HexString.PREFIX)) string else "${HexString.PREFIX}${string}"
    private fun withoutHexPrefix(string: String): String = string.removePrefix(HexString.PREFIX)
}