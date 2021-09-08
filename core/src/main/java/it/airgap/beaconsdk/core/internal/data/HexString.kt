package it.airgap.beaconsdk.core.internal.data

import it.airgap.beaconsdk.core.internal.utils.isHex
import java.math.BigInteger

@JvmInline
internal value class HexString(private val value: String) {
    init {
        require(isValid(value))
    }

    fun length(withPrefix: Boolean = false): Int = asString(withPrefix).length

    fun asString(withPrefix: Boolean = false): String {
        val startsWithPrefix = value.startsWith(PREFIX)

        return when {
            withPrefix && !startsWithPrefix -> "$PREFIX${value}"
            !withPrefix && startsWithPrefix -> value.removePrefix(PREFIX)
            else -> value
        }
    }

    fun toByteArray(): ByteArray = asString().chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    fun toInt(): Int = asString().toUInt(16).toInt()
    fun toBigInteger(): BigInteger = BigInteger(asString(), 16)

    fun slice(indices: IntRange): HexString = HexString(value.slice(indices))
    fun slice(startIndex: Int): HexString = HexString(value.substring(startIndex))

    fun decodeToString(): String = toByteArray().decodeToString()

    companion object {
        const val PREFIX = "0x"

        fun isValid(string: String): Boolean = string.isHex()
    }
}