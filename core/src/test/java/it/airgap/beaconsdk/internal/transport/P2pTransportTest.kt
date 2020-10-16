package it.airgap.beaconsdk.internal.transport

import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.data.p2p.P2pPairingRequest
import it.airgap.beaconsdk.data.sdk.Origin
import it.airgap.beaconsdk.internal.message.ConnectionMessage
import it.airgap.beaconsdk.internal.storage.ExtendedStorage
import it.airgap.beaconsdk.internal.storage.Storage
import it.airgap.beaconsdk.internal.transport.client.P2pCommunicationClient
import it.airgap.beaconsdk.internal.transport.data.TransportMessage
import it.airgap.beaconsdk.internal.utils.*
import it.airgap.beaconsdk.storage.MockBeaconStorageKtx
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class P2pTransportTest {

    @MockK
    private lateinit var p2pCommunicationClient: P2pCommunicationClient

    private lateinit var storage: ExtendedStorage
    private lateinit var p2pTransport: Transport

    private val appName: String = "mockApp"

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockkStatic("it.airgap.beaconsdk.internal.utils.LogKt")
        every { logDebug(any(), any()) } returns Unit

        storage = ExtendedStorage(Storage.KtxDecorator(MockBeaconStorageKtx()))
        p2pTransport = P2pTransport(appName, storage, p2pCommunicationClient)
    }

    @Test
    fun `subscribes for messages from known peers`() {
        val peers = validKnownPeers
        val peersPublicKeys = peers.map { HexString.fromString(it.publicKey) }

        val (transportMessages, transportMessageFlows) =
            messagesAndFlows(peersPublicKeys)

        every { p2pCommunicationClient.subscribeTo(any()) } answers {
            transportMessageFlows.getValue(firstArg<HexString>().value(withPrefix = false))
        }

        runBlocking { storage.setP2pPeers(peers) }

        val expected = transportMessages.map { ConnectionMessage(Origin.P2P(it.id), it.content) }
        val messages = runBlocking {
            p2pTransport.subscribe()
                .onStart { transportMessageFlows.tryEmit(transportMessages) }
                .mapNotNull { it.getOrNull() }
                .take(transportMessages.size)
                .toList()
        }
        verify(exactly = peers.size) { p2pCommunicationClient.subscribeTo(match { peersPublicKeys.contains(it) }) }
        assertEquals(expected.sortedBy { it.content }, messages.sortedBy { it.content })
    }

    @Test
    fun `sends message to specified peer`() {
        val peers = validKnownPeers

        val message = "message"
        val recipient = peers.shuffled().first().publicKey

        coEvery { p2pCommunicationClient.sendTo(any(), any()) } returns internalSuccess(Unit)

        runBlocking { storage.setP2pPeers(peers) }
        runBlocking { p2pTransport.send(message, recipient) }

        coVerify(exactly = 1) { p2pCommunicationClient.sendTo(any(), any()) }
        coVerify { p2pCommunicationClient.sendTo(HexString.fromString(recipient), message) }
    }

    @Test
    fun `broadcasts message to known peers if recipient not specified`() {
        val peers = validKnownPeers
        val recipients = peers.map { HexString.fromString(it.publicKey) }

        val message = "message"

        coEvery { p2pCommunicationClient.sendTo(any(), any()) } returns internalSuccess(Unit)

        runBlocking { storage.setP2pPeers(peers) }
        runBlocking { p2pTransport.send(message).getOrThrow() }

        val actualRecipients = mutableListOf<HexString>()

        coVerify(exactly = peers.size) { p2pCommunicationClient.sendTo(capture(actualRecipients), message) }
        assertEquals(recipients, actualRecipients)
    }

    @Test
    fun `fails to send message if specified recipient is unknown`() {
        val (knownPeers, unknownPeer) = validKnownPeers.splitAt { it.lastIndex - 1 }

        val recipient = unknownPeer.first().publicKey
        val message = "message"

        coEvery { p2pCommunicationClient.sendTo(any(), any()) } returns internalSuccess(Unit)

        runBlocking { storage.setP2pPeers(knownPeers) }

        assertFailsWith(IllegalArgumentException::class) {
            runBlocking { p2pTransport.send(message, recipient).getOrThrow() }
        }
    }

    private val validKnownPeers = listOf(
        P2pPairingRequest("peer1", "0x00", "relayServer"),
        P2pPairingRequest("peer2", "0x01", "relayServer")
    )

    private fun messagesAndFlows(publicKeys: List<HexString>)
            : Pair<List<TransportMessage>, Map<String, MutableSharedFlow<InternalResult<TransportMessage>>>> {

        val transportMessages = publicKeys.mapIndexed { index, hexString ->
            TransportMessage(hexString.value(withPrefix = false), "content$index")
        }

        val transportMessageFlows = publicKeys.map {
            it.value(withPrefix = false) to MutableSharedFlow<InternalResult<TransportMessage>>(transportMessages.size + 1)
        } .toMap()

        return Pair(transportMessages, transportMessageFlows)
    }

    private fun Map<String, MutableSharedFlow<InternalResult<TransportMessage>>>.tryEmit(messages: List<TransportMessage>) {
        messages.forEach { getValue(it.id).tryEmit(internalSuccess(it)) }
    }
}