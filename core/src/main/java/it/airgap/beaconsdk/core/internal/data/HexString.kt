package it.airgap.beaconsdk.core.internal.data

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.utils.isHex
import java.math.BigInteger

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class HexString(private val value: String) {
    init {
        require(isValid(value))
    }

    public fun length(withPrefix: Boolean = false): Int = asString(withPrefix).length

    public fun asString(withPrefix: Boolean = false): String {
        val startsWithPrefix = value.startsWith(PREFIX)

        return when {
            withPrefix && !startsWithPrefix -> "$PREFIX${value}"
            !withPrefix && startsWithPrefix -> value.removePrefix(PREFIX)
            else -> value
        }
    }

    public fun toByteArray(): ByteArray = asString().chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    public fun toInt(): Int = asString().toUInt(16).toInt()
    public fun toBigInteger(): BigInteger = BigInteger(asString().ifEmpty { "00" }, 16)

    public fun slice(indices: IntRange): HexString = HexString(value.slice(indices))
    public fun slice(startIndex: Int): HexString = HexString(value.substring(startIndex))

    public fun decodeToString(): String = toByteArray().decodeToString()

    public companion object {
        public const val PREFIX: String = "0x"

        public fun isValid(string: String): Boolean = string.isHex()
    }
}