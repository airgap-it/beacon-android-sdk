package it.airgap.beaconsdk.internal.transport.p2p

import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.data.beacon.Origin
import it.airgap.beaconsdk.data.beacon.P2pPeerInfo
import it.airgap.beaconsdk.internal.message.SerializedConnectionMessage
import it.airgap.beaconsdk.internal.storage.MockStorage
import it.airgap.beaconsdk.internal.storage.decorator.DecoratedExtendedStorage
import it.airgap.beaconsdk.internal.transport.Transport
import it.airgap.beaconsdk.internal.transport.p2p.data.P2pMessage
import it.airgap.beaconsdk.internal.utils.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import mockLog
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class P2pTransportTest {

    @MockK
    private lateinit var p2pClient: P2pClient

    private lateinit var storage: DecoratedExtendedStorage
    private lateinit var p2pTransport: Transport

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockLog()

        coEvery { p2pClient.sendPairingRequest(any(), any(), any()) } returns Success()

        storage = DecoratedExtendedStorage(MockStorage())
        p2pTransport = P2pTransport(storage, p2pClient)
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `subscribes for messages from known peers`() {
        val peers = validKnownPeers
        val peersPublicKeys = peers.map { HexString.fromString(it.publicKey) }

        val (transportMessages, transportMessageFlows) =
            messagesAndFlows(peersPublicKeys)

        every { p2pClient.subscribeTo(any()) } answers {
            transportMessageFlows.getValue(firstArg<HexString>().value())
        }

        runBlocking { storage.setP2pPeers(peers) }

        val expected = transportMessages.map { SerializedConnectionMessage(Origin.P2P(it.id), it.content) }
        val messages = runBlocking {
            p2pTransport.subscribe()
                .onStart { transportMessageFlows.tryEmit(transportMessages) }
                .mapNotNull { it.valueOrNull() }
                .filterIsInstance<SerializedConnectionMessage>()
                .take(transportMessages.size)
                .toList()
        }
        verify(exactly = peers.size) { p2pClient.subscribeTo(match { peersPublicKeys.contains(it) }) }
        assertEquals(expected.sortedBy { it.content }, messages.sortedBy { it.content })
    }

    @Test
    fun `sends message to specified peer`() {
        val peers = validKnownPeers

        val message = "message"
        val recipient = peers.shuffled().first().publicKey

        coEvery { p2pClient.sendTo(any(), any()) } returns Success()

        runBlocking { storage.setP2pPeers(peers) }
        runBlocking { p2pTransport.send(message, recipient) }

        coVerify(exactly = 1) { p2pClient.sendTo(any(), any()) }
        coVerify { p2pClient.sendTo(HexString.fromString(recipient), message) }
    }

    @Test
    fun `broadcasts message to known peers if recipient not specified`() {
        val peers = validKnownPeers
        val recipients = peers.map { HexString.fromString(it.publicKey) }

        val message = "message"

        coEvery { p2pClient.sendTo(any(), any()) } returns Success()

        runBlocking { storage.setP2pPeers(peers) }
        runBlocking { p2pTransport.send(message).value() }

        val actualRecipients = mutableListOf<HexString>()

        coVerify(exactly = peers.size) { p2pClient.sendTo(capture(actualRecipients), message) }
        assertEquals(recipients, actualRecipients)
    }

    @Test
    fun `fails to send message if specified recipient is unknown`() {
        val (knownPeers, unknownPeer) = validKnownPeers.splitAt { it.lastIndex - 1 }

        val recipient = unknownPeer.first().publicKey
        val message = "message"

        coEvery { p2pClient.sendTo(any(), any()) } returns Success()

        runBlocking { storage.setP2pPeers(knownPeers) }

        assertFailsWith<IllegalArgumentException> {
            runBlocking { p2pTransport.send(message, recipient).value() }
        }
    }

    @Test
    fun `subscribes for messages from new peers`() {
        val peers = validKnownPeers
        val peersPublicKeys = peers.map { HexString.fromString(it.publicKey) }

        val (transportMessages, transportMessageFlows) =
            messagesAndFlows(peersPublicKeys)

        every { p2pClient.subscribeTo(any()) } answers {
            transportMessageFlows.getValue(firstArg<HexString>().value())
        }

        val expected = transportMessages.map { SerializedConnectionMessage(Origin.P2P(it.id), it.content) }

        runBlocking {
            val messages = async {
                p2pTransport.subscribe()
                    .onStart { transportMessageFlows.tryEmit(transportMessages) }
                    .mapNotNull { it.valueOrNull() }
                    .filterIsInstance<SerializedConnectionMessage>()
                    .take(transportMessages.size)
                    .toList()
            }

            storage.addP2pPeers(peers)

            assertEquals(expected.sortedBy { it.content }, messages.await().sortedBy { it.content })
            verify(exactly = peers.size) {
                p2pClient.subscribeTo(match { peersPublicKeys.contains(it) })
            }
        }
    }

    @Test
    fun `pairs with new peers`() {
        val peers = validNewPeers
        val peersPublicKeys = peers.map { HexString.fromString(it.publicKey) }
        val peersRelayServers = peers.map { it.relayServer }
        val peersVersions = peers.map { it.version }

        val (transportMessages, transportMessageFlows) =
            messagesAndFlows(peersPublicKeys)

        every { p2pClient.subscribeTo(any()) } answers {
            transportMessageFlows.getValue(firstArg<HexString>().value())
        }

        runBlocking {
            val messages = async {
                p2pTransport.subscribe()
                    .onStart { transportMessageFlows.tryEmit(transportMessages) }
                    .take(transportMessages.size)
                    .collect()
            }

            storage.addP2pPeers(peers)
            messages.await()

            val expectedInStorage = peers.map { it.copy(isPaired = true) }
            val fromStorage = async {
                storage.p2pPeers
                    .filter { it.isPaired }
                    .take(peers.size)
                    .toList()
            }

            assertEquals(expectedInStorage.sortedBy { it.name }, fromStorage.await().sortedBy { it.name })
            coVerify(exactly = peers.filter { !it.isPaired }.size) {
                p2pClient.sendPairingRequest(
                    match { peersPublicKeys.contains(it) },
                    match { peersRelayServers.contains(it) },
                    matchNullable { peersVersions.contains(it) },
                )
            }
        }
    }

    @Test
    fun `does not pair with known peers`() {
        val peers = validKnownPeers
        val peersPublicKeys = peers.map { HexString.fromString(it.publicKey) }

        val (transportMessages, transportMessageFlows) =
            messagesAndFlows(peersPublicKeys)

        every { p2pClient.subscribeTo(any()) } answers {
            transportMessageFlows.getValue(firstArg<HexString>().value())
        }

        runBlocking {
            storage.setP2pPeers(peers)
            val messages = async {
                p2pTransport.subscribe()
                    .onStart { transportMessageFlows.tryEmit(transportMessages) }
                    .take(transportMessages.size)
                    .collect()
            }

            messages.await()

            val fromStorage = storage.getP2pPeers()

            coVerify(exactly = 0) { p2pClient.sendPairingRequest(any(), any(), any()) }
            assertEquals(peers.sortedBy { it.name }, fromStorage.sortedBy { it.name })
        }
    }

    @Test
    fun `does not update new pairs if could not pair`() {
        val peers = validNewPeers
        val peersPublicKeys = peers.map { HexString.fromString(it.publicKey) }
        val peersRelayServer = peers.map { it.relayServer }
        val peersVersions = peers.map { it.version }

        val (transportMessages, transportMessageFlows) =
            messagesAndFlows(peersPublicKeys)

        coEvery { p2pClient.sendPairingRequest(any(), any(), any()) } returns Failure()
        every { p2pClient.subscribeTo(any()) } answers {
            transportMessageFlows.getValue(firstArg<HexString>().value())
        }

        runBlocking {
            val messages = async {
                p2pTransport.subscribe()
                    .onStart { transportMessageFlows.tryEmit(transportMessages) }
                    .take(transportMessages.size)
                    .collect()
            }

            storage.addP2pPeers(peers)
            messages.await()

            val expectedInStorage = peers.map { it.copy(isPaired = false) }
            val fromStorage = storage.getP2pPeers()

            coVerify(exactly = peers.filter { !it.isPaired }.size) {
                p2pClient.sendPairingRequest(
                    match { peersPublicKeys.contains(it) },
                    match { peersRelayServer.contains(it) },
                    matchNullable { peersVersions.contains(it) },
                )
            }
            assertEquals(expectedInStorage.sortedBy { it.name }, fromStorage.sortedBy { it.name })
        }
    }

    @Test
    fun `does not subscribe to already subscribed peer`() {
        val peers = validNewPeers
        val peersPublicKeys = peers.map { HexString.fromString(it.publicKey) }

        val (transportMessages, transportMessageFlows) =
            messagesAndFlows(peersPublicKeys)

        every { p2pClient.subscribeTo(any()) } answers {
            transportMessageFlows.getValue(firstArg<HexString>().value())
        }

        runBlocking {
            val messages = async {
                p2pTransport.subscribe()
                    .onStart { transportMessageFlows.tryEmit(transportMessages) }
                    .take(transportMessages.size)
                    .collect()
            }

            storage.addP2pPeers(peers)

            messages.await()

            val fromStorage = async {
                storage.p2pPeers
                    .filter { it.isPaired }
                    .take(peers.size)
                    .toList()
            }

            fromStorage.await()

            coVerify(exactly = peers.size) {
                p2pClient.subscribeTo(match { peersPublicKeys.contains(it) })
            }
        }
    }

    private val validKnownPeers = listOf(
        P2pPeerInfo("peer1", "0x00", "relayServer", isPaired = true),
        P2pPeerInfo("peer2", "0x01", "relayServer", "1", isPaired = true),
        P2pPeerInfo("peer2", "0x02", "relayServer", "2", isPaired = true),
    )

    private val validNewPeers = listOf(
        P2pPeerInfo("peer1", "0x00", "relayServer", isPaired = false),
        P2pPeerInfo("peer2", "0x01", "relayServer", "1", isPaired = false),
        P2pPeerInfo("peer2", "0x02", "relayServer", "2", isPaired = false),
    )

    private fun messagesAndFlows(publicKeys: List<HexString>)
            : Pair<List<P2pMessage>, Map<String, MutableSharedFlow<InternalResult<P2pMessage>>>> {

        val transportMessages = publicKeys.mapIndexed { index, hexString ->
            P2pMessage(hexString.value(), "content$index")
        }

        val transportMessageFlows = publicKeys.map {
            it.value() to MutableSharedFlow<InternalResult<P2pMessage>>(transportMessages.size + 1)
        } .toMap()

        return Pair(transportMessages, transportMessageFlows)
    }

    private fun Map<String, MutableSharedFlow<InternalResult<P2pMessage>>>.tryEmit(messages: List<P2pMessage>) {
        messages.forEach { getValue(it.id).tryEmit(Success(it)) }
    }
}