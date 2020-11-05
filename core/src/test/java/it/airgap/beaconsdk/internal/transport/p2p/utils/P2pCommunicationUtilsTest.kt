package it.airgap.beaconsdk.internal.transport.p2p.utils

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.transport.p2p.data.P2pHandshakeInfo
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixEvent
import it.airgap.beaconsdk.internal.utils.HexString
import it.airgap.beaconsdk.internal.utils.Success
import it.airgap.beaconsdk.internal.utils.asHexString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class P2pCommunicationUtilsTest {

    @MockK
    private lateinit var crypto: Crypto

    private lateinit var p2pCommunicationUtils: P2pCommunicationUtils

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { crypto.hashKey(any<HexString>()) } answers { Success(firstArg<HexString>().asByteArray()) }

        p2pCommunicationUtils = P2pCommunicationUtils(crypto)
    }

    @Test
    fun `creates recipient id`() {
        val publicKey = HexString.fromString("e40476d7")
        val relayServer = "relayServer"

        val recipientIdentifier = p2pCommunicationUtils.recipientIdentifier(publicKey, relayServer)

        assertEquals("@${publicKey.value()}:$relayServer", recipientIdentifier)
    }

    @Test
    fun `checks if message is coming from specified sender`() {
        val expectedSender = HexString.fromString("e40476d7")
        val unexpectedSender = HexString.fromString("e40476d8")

        val expectedIdentifier = p2pCommunicationUtils.recipientIdentifier(expectedSender, "relayServer")
        val unexpectedIdentifier = p2pCommunicationUtils.recipientIdentifier(unexpectedSender, "relayServer")

        val fromExpected = MatrixEvent.TextMessage("roomId", expectedIdentifier, "message")
        val fromUnexpected = MatrixEvent.TextMessage("roomId", unexpectedIdentifier, "message")

        assertTrue(
            p2pCommunicationUtils.isMessageFrom(fromExpected, expectedSender),
            "Expected message to be recognized as coming from the specified sender"
        )
        assertFalse(p2pCommunicationUtils.isMessageFrom(fromUnexpected, expectedSender),
            "Expected message to be recognized as not coming from the specified sender"
        )
    }

    @Test
    fun `creates pairing request payload for specified version`() {
        val appName = "mockApp"
        val publicKey = byteArrayOf(0)
        val relayServer = "relayServer"

        versionsWithPairingPayloads(appName, publicKey, relayServer)
            .map { p2pCommunicationUtils.pairingPayload(it.first, appName, publicKey, relayServer) to it.second }
            .forEach { assertEquals(it.second, it.first) }
    }

    @Test
    fun `creates channel opening message`() {
        val recipient = "recipient"
        val payload = "payload"

        val message = p2pCommunicationUtils.channelOpeningMessage(recipient, payload)

        assertEquals("@channel-open:$recipient:$payload", message)
    }

    private fun versionsWithPairingPayloads(
        appName: String = "mockApp",
        publicKey: ByteArray = byteArrayOf(0),
        relayServer: String = "relayServer",
    ): List<Pair<String, String>> = listOf(
        "1" to publicKey.asHexString().value(),
        "1.0" to publicKey.asHexString().value(),
        "1.0.0" to publicKey.asHexString().value(),
        "1.abc" to publicKey.asHexString().value(),
        "2" to Json.encodeToString(P2pHandshakeInfo(appName, "2", publicKey.asHexString().value(), relayServer)),
        "2.0" to Json.encodeToString(P2pHandshakeInfo(appName, "2.0", publicKey.asHexString().value(), relayServer)),
        "2.0.0" to Json.encodeToString(P2pHandshakeInfo(appName, "2.0.0", publicKey.asHexString().value(), relayServer)),
        "2.abc" to Json.encodeToString(P2pHandshakeInfo(appName, "2.abc", publicKey.asHexString().value(), relayServer)),
        "3" to Json.encodeToString(P2pHandshakeInfo(appName, "3", publicKey.asHexString().value(), relayServer)),
    )
}