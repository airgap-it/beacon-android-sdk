package it.airgap.beaconsdk.transport.p2p.matrix.internal.data

import it.airgap.beaconsdk.core.internal.transport.p2p.data.P2pIdentifier
import it.airgap.beaconsdk.core.internal.transport.p2p.data.p2pIdentifierOrNull
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

internal class P2pIdentifierTest {

    @Test
    fun `creates P2P identifier from public key hash and relay server`() {
        val publicKeyHash = byteArrayOf(1, 2, 3, 4, 5)
        val relayServer = "relay.server:port"

        val identifier = P2pIdentifier(publicKeyHash, relayServer)

        assertEquals(P2pIdentifier("@0102030405:relay.server:port"), identifier)
    }

    @Test
    fun `creates P2P identifier or returns null if invalid`() {
        val valid = "@0102030405:relay.server:port"
        val invalid = "invalidIdentifier"

        assertEquals(P2pIdentifier("@0102030405:relay.server:port"), p2pIdentifierOrNull(valid))
        assertNull(p2pIdentifierOrNull(invalid))
    }

    @Test
    fun `checks if string is valid P2P identifier`() {
        val identifiersWithExpected = listOf(
            "@0102030405:relay.server:port" to true,
            "@0102030405:relay.server" to true,
            "0102030405:relay.server:port" to false,
            "@0102030405relay.server" to false,
            "0102030405relay.server" to false,
            "$0102030405,relay.server" to false,
        )

        identifiersWithExpected
            .map { P2pIdentifier.isValid(it.first) to it.second }
            .forEach { assertEquals(it.second, it.first, "Expected P2pIdentifier.isValid result to match the expected value") }
    }

    @Test
    fun `fails if created with invalid P2P identifier`() {
        assertFailsWith<IllegalArgumentException> { P2pIdentifier("invalidIdentifier") }
    }

    @Test
    fun `separates public key hash and relay server in separate properties`() {
        val publicKeyHash = "0102030405"
        val relayServer = "relay.server:port"

        val p2pIdentifier = P2pIdentifier("@$publicKeyHash:$relayServer")

        assertEquals(publicKeyHash, p2pIdentifier.publicKeyHash)
        assertEquals(relayServer, p2pIdentifier.relayServer)
    }
}