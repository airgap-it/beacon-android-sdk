package it.airgap.beaconsdk.internal.transport.p2p.utils

import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.utils.HexString
import it.airgap.beaconsdk.internal.utils.asHexString
import java.math.BigInteger

internal class P2pServerUtils(private val crypto: Crypto, private val matrixNodes: List<String>) {

    fun getRelayServer(publicKey: ByteArray, nonce: HexString? = null): String {
        return getRelayServer(publicKey.asHexString(), nonce)
    }

    fun getRelayServer(publicKey: HexString, nonce: HexString? = null): String {
        val hash = crypto.hashKey(publicKey).get().asHexString()
        val nonceValue = nonce?.asString() ?: ""

        return matrixNodes.map(this::getRelayServerHostname).minByOrNull {
            val relayServerHash = crypto.hash(it + nonceValue, 32).get().asHexString()
            hash.absDiff(relayServerHash)
        } ?: ""
    }

    private fun getRelayServerHostname(relayServer: String): String =
        relayServer.substringAfter("://").substringBefore("/")

    private fun HexString.absDiff(other: HexString): BigInteger =
        (toBigInteger() - other.toBigInteger()).abs()
}