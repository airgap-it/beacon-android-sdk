package it.airgap.beaconsdk.internal.transport.p2p.data

import it.airgap.beaconsdk.internal.utils.toHexString

private const val PREFIX = "@"
private const val SEPARATOR = ":"

private val identifierRegex: Regex = Regex("^$PREFIX(.+)$SEPARATOR(.+)$")

@JvmInline
internal value class P2pIdentifier(private val value: String) {
    init {
        require(isValid(value))
    }

    private val parts: List<String> get() = value.split(SEPARATOR, limit = 2).also { require(it.size == 2) }
    val publicKeyHash: String get() = parts[0].removePrefix(PREFIX)
    val relayServer: String get() = parts[1]

    fun asString(): String = value

    companion object {
        fun isValid(value: String): Boolean = identifierRegex.matches(value)
    }
}

internal fun P2pIdentifier(publicKeyHash: ByteArray, relayServer: String): P2pIdentifier =
    P2pIdentifier("$PREFIX${publicKeyHash.toHexString().asString()}$SEPARATOR$relayServer")

internal fun p2pIdentifierOrNull(value: String): P2pIdentifier? =
    if (P2pIdentifier.isValid(value)) P2pIdentifier(value) else null