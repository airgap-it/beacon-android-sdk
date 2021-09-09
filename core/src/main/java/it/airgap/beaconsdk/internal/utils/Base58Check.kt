package it.airgap.beaconsdk.internal.utils

import it.airgap.beaconsdk.internal.crypto.Crypto
import java.math.BigInteger

internal class Base58Check(private val crypto: Crypto) {
    private val bs58Regex = Regex("^[${ALPHABET}]+$")

    private val bi0: BigInteger by lazy { BigInteger.ZERO }
    private val biAlphabet: BigInteger by lazy { BigInteger.valueOf(ALPHABET.length.toLong()) }

    fun encode(bytes: ByteArray): Result<String> =
        runCatching {
            val checksum = createChecksum(bytes)
            val encoded = bytesToBase58(bytes + checksum)

            encoded
        }

    fun decode(base58check: String): Result<ByteArray> =
        runCatching {
            if (!base58check.matches(bs58Regex)) failWithInvalidString()

            val (payloadList, checksumList) = bytesFromBase58(base58check).toList()
                .splitAt { it.size - 4 }
            val payload = payloadList.toByteArray()
            val checksum = checksumList.toByteArray()

            val newChecksum = createChecksum(payload)
            if (!checksum.contentEquals(newChecksum)) failWithInvalidChecksum()

            payload
        }

    private fun bytesToBase58(bytes: ByteArray): String {
        val hex = bytes.toHexString()
        val base58 = bigIntegerToBase58("", hex.toBigInteger()).reversed()

        val leadingZeros = bytes.takeWhile { it == 0.toByte() }.size

        return ALPHABET[0].toString().repeat(leadingZeros) + base58
    }

    private fun bytesFromBase58(base58: String): ByteArray {
        val chars = base58.toCharArray().toList()
        val bytes = base58ToBigInteger(bi0, chars).toHexString().toByteArray()

        val leadingZeros = base58.takeWhile { it == ALPHABET[0] }.length

        return ByteArray(leadingZeros) { 0 } + bytes
    }

    private fun createChecksum(bytes: ByteArray): ByteArray {
        val hash = crypto.hashSha256(crypto.hashSha256(bytes).getOrThrow()).getOrThrow()

        return hash.sliceArray(0 until 4)
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

    private fun failWithInvalidString(): Nothing = throw IllegalArgumentException("Base58Check string contains invalid characters")
    private fun failWithInvalidChecksum(): Nothing = throw IllegalArgumentException("Base58Check checksum is invalid")

    companion object {
        private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    }
}