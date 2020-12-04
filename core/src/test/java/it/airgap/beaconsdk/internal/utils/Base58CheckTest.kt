package it.airgap.beaconsdk.internal.utils

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.internal.crypto.Crypto
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import java.security.MessageDigest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class Base58CheckTest {

    @MockK
    private lateinit var crypto: Crypto

    private lateinit var base58Check: Base58Check

    private val bytesWithEncodings = listOf(
        "00" to "1Wh4bh",
        "9434dc98" to "Rnnju5zumgo",
        "1b25632c" to "5YMLYNMq9ZC",
        "511e22c1" to "EZwirJx8Yjz",
        "fb1e19ce" to "j19zm4WDXKc",
        "260bf0ed" to "7N6onopA64Y",
        "2317951c" to "6sSNnuYWSuh",
        "07797720fbc2" to "RMeHkvFFSKtD6",
        "baf281bc0c9f" to "BWBDpE67R1vcie",
        "9f4ef22b48c8" to "9x7b6fEpiurKjL",
        "0063f15efd" to "1Hia7VA7ut1T",
        "000008d64e1eaa" to "117XQCFEaGaFjD",
        "0000000b1e92e6ac" to "1119D8LFdoz3aG5",
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

    private val invalidChecksums = listOf(
        "1Wh4rh",
        "Rnnju5zum7o",
        "5YMLYNMq9Zg",
        "EZwirJx8Yab",
        "j19zm4WD3Kc",
        "7N6onopA64Yp",
        "6sSNnuYWSuhb",
        "RMeHkvFFSKtQD6",
        "BWBDpE67R1v9cie",
        "9x7Bb6fEpiurKjL",
        "1Hi6a7VA7ut1T",
        "117XQeCFEaGaFjD",
        "111p9D8LFdoz3aG5",
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        val messageDigest = MessageDigest.getInstance("SHA-256")

        every { crypto.hashSha256(any<ByteArray>()) } answers { Success(messageDigest.digest(firstArg())) }
        every { crypto.hashSha256(any<HexString>()) } answers { Success(messageDigest.digest(firstArg<HexString>().asByteArray())) }

        base58Check = Base58Check(crypto)
    }

    @Test
    fun `encodes bytes to Base58Check string`() {
        val bytesWithExpected = bytesWithEncodings.map { HexString.fromString(it.first).asByteArray() to it.second }

        bytesWithExpected
            .map { base58Check.encode(it.first).get() to it.second }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `decodes Base58Check string to bytes`() {
        val encodedWithExpected = bytesWithEncodings.map { it.second to HexString.fromString(it.first).asByteArray() }

        encodedWithExpected
            .map { base58Check.decode(it.first).get() to it.second }
            .forEach {
                assertArrayEquals(it.second, it.first)
            }
    }

    @Test
    fun `fails when decoding invalid base58 string`() {
        invalidBase58Strings.forEach {
            assertFailsWith<IllegalArgumentException> {
                base58Check.decode(it).get()
            }
        }
    }

    @Test
    fun `fails on checksum mismatch`() {
        invalidChecksums.forEach {
            assertFailsWith<IllegalArgumentException> {
                base58Check.decode(it).get()
            }
        }
    }
}