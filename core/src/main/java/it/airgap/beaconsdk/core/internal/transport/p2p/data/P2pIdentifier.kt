package it.airgap.beaconsdk.core.internal.transport.p2p.data

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.utils.toHexString

private const val PREFIX = "@"
private const val SEPARATOR = ":"

private val identifierRegex: Regex = Regex("^$PREFIX(.+)$SEPARATOR(.+)$")

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class P2pIdentifier(private val value: String) {
    init {
        require(isValid(value))
    }

    private val parts: List<String> get() = value.split(SEPARATOR, limit = 2).also { require(it.size == 2) }
    public val publicKeyHash: String get() = parts[0].removePrefix(PREFIX)
    public val relayServer: String get() = parts[1]

    public fun asString(): String = value

    public companion object {
        public fun isValid(value: String): Boolean = identifierRegex.matches(value)
    }
}

public fun P2pIdentifier(publicKeyHash: ByteArray, relayServer: String): P2pIdentifier =
    P2pIdentifier("$PREFIX${publicKeyHash.toHexString().asString()}$SEPARATOR$relayServer")

public fun p2pIdentifierOrNull(value: String): P2pIdentifier? =
    if (P2pIdentifier.isValid(value)) P2pIdentifier(value) else null