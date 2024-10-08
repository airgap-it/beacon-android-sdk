package it.airgap.beaconsdk.core.internal.utils

import io.mockk.MockKAnnotations
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class Base58Test {

    private lateinit var base58: Base58

    private val bytesWithEncodings = listOf(
        "00" to "1",
        "9434dc98" to "4nivY7",
        "1b25632c" to "hFDyV",
        "511e22c1" to "35G7vG",
        "fb1e19ce" to "7RJ19P",
        "260bf0ed" to "yQYLk",
        "2317951c" to "u2VHd",
        "07797720fbc2" to "4it3UV4m",
        "baf281bc0c9f" to "2c6UCNvaS",
        "9f4ef22b48c8" to "2NLCDoVWw",
        "0063f15efd" to "13ZArBn",
        "000008d64e1eaa" to "11zpyELy",
        "0000000b1e92e6ac" to "1112FmB6tT",
    )

    private val invalidBase58Strings = listOf(
        "",
        "01Wh4bh",
        "RnnjuO5zumgo",
        "5YIMLYNMq9ZC",
        "EZwirJx8Yjlz",
        "j19zm4WDX0Kc",
        "7N6oOnopA64Y",
        "6sSNnluYWSuh",
        "RMIeHkvFFSKtD6",
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        base58 = Base58()
    }

    @Test
    fun `encodes bytes to Base58 string`() {
        val bytesWithExpected = bytesWithEncodings.map { it.first.asHexString().toByteArray() to it.second }

        bytesWithExpected
            .map { base58.encode(it.first).getOrThrow() to it.second }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `decodes Base58 string to bytes`() {
        val encodedWithExpected = bytesWithEncodings.map { it.second to it.first.asHexString().toByteArray() }

        encodedWithExpected
            .map { base58.decode(it.first).getOrThrow() to it.second }
            .forEach {
                assertArrayEquals(it.second, it.first)
            }
    }

    @Test
    fun `fails when decoding invalid Base58 string`() {
        invalidBase58Strings.forEach {
            assertFailsWith<IllegalArgumentException> {
                base58.decode(it).getOrThrow()
            }
        }
    }
}
