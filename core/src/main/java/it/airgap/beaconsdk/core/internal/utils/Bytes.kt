package it.airgap.beaconsdk.core.internal.utils

import it.airgap.beaconsdk.core.internal.data.HexString
import java.math.BigInteger

private val hexRegex: Regex = Regex("^(${HexString.PREFIX})?([0-9a-fA-F]{2})+$")

internal fun String.isHex(): Boolean = this.matches(hexRegex)

internal fun String.asHexStringOrNull(): HexString? = if (isHex()) HexString(this) else null
internal fun String.asHexString(): HexString = asHexStringOrNull() ?: failWithInvalidHexString(this)

internal fun String.encodeToHexString(): HexString = encodeToByteArray().toHexString()

internal fun Byte.toHexString(): HexString =
    toUByte().toString(16).padStartEven('0').asHexString()

internal fun ByteArray.toHexString(): HexString =
    joinToString("") { it.toHexString().asString() }.asHexString()

internal fun ByteArray.toBigInteger(): BigInteger = BigInteger(this)

internal fun List<Byte>.toHexString(): HexString =
    joinToString("") { it.toHexString().asString() }.asHexString()

internal fun Int.toHexString(): HexString =
    toUInt().toString(16).padStartEven('0').asHexString()

internal fun BigInteger.toHexString(): HexString =
    asHexStringOrNull() ?: failWithNegativeNumber(this)

internal fun BigInteger.asHexStringOrNull(): HexString? =
    if (this >= BigInteger.ZERO) toString(16).padStartEven('0').asHexString()
    else null

private fun failWithNegativeNumber(number: BigInteger): Nothing =
    throw IllegalArgumentException("cannot create HexString from a negative number $number")

private fun failWithInvalidHexString(string: String): Nothing =
    throw IllegalArgumentException("$string is not a valid hex string")