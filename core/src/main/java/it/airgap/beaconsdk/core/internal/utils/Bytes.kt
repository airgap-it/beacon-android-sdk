package it.airgap.beaconsdk.core.internal.utils

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.data.HexString
import java.math.BigInteger

private val hexRegex: Regex = Regex("^(${HexString.PREFIX})?([0-9a-fA-F]{2})+$")

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun String.isHex(): Boolean = this.matches(hexRegex)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun String.asHexStringOrNull(): HexString? = if (isHex()) HexString(this) else null

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun String.asHexString(): HexString = asHexStringOrNull() ?: failWithInvalidHexString(this)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun String.encodeToHexString(): HexString = encodeToByteArray().toHexString()

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Byte.toHexString(): HexString =
    toUByte().toString(16).padStartEven('0').asHexString()

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun ByteArray.toHexString(): HexString =
    joinToString("") { it.toHexString().asString() }.asHexString()

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun ByteArray.toBigInteger(): BigInteger = BigInteger(this)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun List<Byte>.toHexString(): HexString =
    joinToString("") { it.toHexString().asString() }.asHexString()

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Int.toHexString(): HexString =
    toUInt().toString(16).padStartEven('0').asHexString()

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun BigInteger.toHexString(): HexString =
    asHexStringOrNull() ?: failWithNegativeNumber(this)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun BigInteger.asHexStringOrNull(): HexString? =
    if (this >= BigInteger.ZERO) toString(16).padStartEven('0').asHexString()
    else null

private fun failWithNegativeNumber(number: BigInteger): Nothing =
    throw IllegalArgumentException("cannot create HexString from a negative number $number")

private fun failWithInvalidHexString(string: String): Nothing =
    throw IllegalArgumentException("$string is not a valid hex string")