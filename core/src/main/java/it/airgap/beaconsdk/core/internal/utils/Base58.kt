package it.airgap.beaconsdk.core.internal.utils

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import java.math.BigInteger

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Base58 {
    private val bs58Regex = Regex("^[${ALPHABET}]+$")

    private val bi0: BigInteger by lazy { BigInteger.ZERO }
    private val biAlphabet: BigInteger by lazy { BigInteger.valueOf(ALPHABET.length.toLong()) }

    public fun encode(bytes: ByteArray): Result<String> =
        runCatching { bytesToBase58(bytes) }

    public fun decode(base58: String): Result<ByteArray> =
        runCatching {
            if (!base58.matches(bs58Regex)) failWithInvalidString()
            bytesFromBase58(base58)
        }

    private fun bytesToBase58(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""

        val hex = bytes.toHexString()
        val base58 = bigIntegerToBase58("", hex.toBigInteger()).reversed()

        val leadingZeros = bytes.takeWhile { it == 0.toByte() }.size

        return ALPHABET[0].toString().repeat(leadingZeros) + base58
    }

    private fun bytesFromBase58(base58: String): ByteArray {
        if (base58.isEmpty()) return ByteArray(0)

        val leadingZeros = base58.takeWhile { it == ALPHABET[0] }.length
        val chars = base58.substring(leadingZeros).toCharArray().toList()

        val bytes =
            if (chars.isNotEmpty()) base58ToBigInteger(bi0, chars).toHexString().toByteArray()
            else ByteArray(0)

        return ByteArray(leadingZeros) { 0 } + bytes
    }

    private tailrec fun bigIntegerToBase58(acc: String, next: BigInteger): String {
        if (next <= bi0) return acc

        val reminder = (next % biAlphabet).toInt()
        return bigIntegerToBase58(acc + ALPHABET[reminder % ALPHABET.length], next / biAlphabet)
    }

    private tailrec fun base58ToBigInteger(acc: BigInteger, next: List<Char>): BigInteger {
        if (next.isEmpty()) return acc

        val char = next.first()
        val reminder = BigInteger.valueOf(ALPHABET.indexOf(char).toLong())

        return base58ToBigInteger(acc * biAlphabet + reminder, next.tail())
    }

    private fun failWithInvalidString(): Nothing = throw IllegalArgumentException("Base58 string contains invalid characters")

    public companion object {
        private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    }
}