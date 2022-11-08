package it.airgap.beaconsdk.core.internal.transport.p2p

import containsLike
import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.data.P2pPeer
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.message.SerializedIncomingConnectionMessage
import it.airgap.beaconsdk.core.internal.message.SerializedOutgoingConnectionMessage
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.transport.Transport
import it.airgap.beaconsdk.core.internal.transport.p2p.data.P2pMessage
import it.airgap.beaconsdk.core.internal.transport.p2p.store.P2pTransportStore
import it.airgap.beaconsdk.core.internal.utils.*
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.transport.p2p.P2pClient
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import mockLog
import onNth
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class P2pTransportTest {

    @MockK
    private lateinit var p2pClient: P2pClient

    @MockK
    private lateinit var identifierCreator: IdentifierCreator

    @MockK
    private lateinit var store: P2pTransportStore

    private lateinit var storageManager: StorageManager
    private lateinit var p2pTransport: Transport

    private val beaconScope: BeaconScope = BeaconScope.Global

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockLog()

        coEvery { p2pClient.sendPairingResponse(any()) } returns Result.success()
        every { identifierCreator.senderId(any()) } answers { Result.success(firstArg<ByteArray>().toHexString().asString()) }

        storageManager = StorageManager(beaconScope, MockStorage(), MockSecureStorage(), identifierCreator, BeaconConfiguration(ignoreUnsupportedBlockchains = false))
        p2pTransport = P2pTransport(storageManager, p2pClient, store)
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `subscribes for messages from known peers`() {
        val peers = validKnownPeers
        val peersPublicKeys = peers.map { it.publicKey.asHexString().toByteArray() }

        val (transportMessages, transportMessageFlows) = messagesAndFlows(peersPublicKeys)

        every { p2pClient.isSubscribed(any()) } returns false
        every { p2pClient.subscribeTo(any()) } answers { transportMessageFlows.getValue(firstArg<P2pPeer>().publicKey.asHexString().asString()) }

        runBlocking { storageManager.setPeers(peers) }

        val expected = transportMessages.map { SerializedIncomingConnectionMessage(Connection.Id.P2P(it.publicKey), it.content) }
        val messages = runBlocking {
            p2pTransport.subscribe()
                .onStart { transportMessageFlows.tryEmit(transportMessages) }
                .mapNotNull { it.getOrNull() }
                .filterIsInstance<SerializedIncomingConnectionMessage>()
                .take(transportMessages.size)
                .toList()
        }
        verify(exactly = peers.size) { p2pClient.subscribeTo(match { peersPublicKeys.containsLike(it.publicKey.asHexString().toByteArray()) }) }
        assertEquals(expected.sortedBy { it.content }, messages.sortedBy { it.content })
    }

    @Test
    fun `unsubscribes from removed peers`() {
        val (subscribed, unsubscribed) = validKnownPeers.splitAt { it.size / 2 }
        val subscribedPublicKeys = subscribed.map { it.publicKey.asHexString().toByteArray() }
        val unsubscribedPublicKeys = unsubscribed.map { it.publicKey.asHexString().toByteArray() }

        val (transportMessages, transportMessageFlows) = messagesAndFlows(subscribedPublicKeys + unsubscribedPublicKeys)

        every { p2pClient.isSubscribed(any()) } returns false
        every { p2pClient.subscribeTo(any()) } answers { transportMessageFlows.getValue(firstArg<P2pPeer>().publicKey.asHexString().asString()) }
        coEvery { p2pClient.unsubscribeFrom(any()) } returns Unit

        runBlocking { storageManager.setPeers(subscribed + unsubscribed) }

        runBlocking {
            p2pTransport.subscribe()
                .onStart { transportMessageFlows.tryEmit(transportMessages) }
                .flatMapMerge { storageManager.updatedPeers }
                .onNth(1) { storageManager.removePeers(unsubscribed) }
                .filter { it.isRemoved }
                .take(unsubscribed.size)
                .collect()
        }

        coVerify(exactly = unsubscribed.size) { p2pClient.unsubscribeFrom(match { unsubscribedPublicKeys.containsLike(it.publicKey.asHexString().toByteArray()) }) }
    }

    @Test
    fun `sends message to specified peer`() {
        val peers = validKnownPeers

        val message = "message"
        val recipient = peers.shuffled().first()
        val serialized = SerializedOutgoingConnectionMessage(Connection.Id.P2P(recipient.publicKey), message)

        coEvery { p2pClient.sendTo(any(), any()) } returns Result.success()

        runBlocking { storageManager.setPeers(peers) }
        runBlocking { p2pTransport.send(serialized) }

        coVerify(exactly = 1) { p2pClient.sendTo(any(), any()) }
        coVerify { p2pClient.sendTo(recipient, message) }
    }

    @Test
    fun `subscribes for messages from new peers`() {
        val peers = validKnownPeers
        val peersPublicKeys = peers.map { it.publicKey.asHexString().toByteArray() }

        val (transportMessages, transportMessageFlows) = messagesAndFlows(peersPublicKeys)

        every { p2pClient.isSubscribed(any()) } returns false
        every { p2pClient.subscribeTo(any()) } answers { transportMessageFlows.getValue(firstArg<P2pPeer>().publicKey.asHexString().asString()) }

        val expected = transportMessages.map { SerializedIncomingConnectionMessage(Connection.Id.P2P(it.publicKey), it.content) }

        runBlocking {
            val messages = async {
                p2pTransport.subscribe()
                    .onStart { transportMessageFlows.tryEmit(transportMessages) }
                    .mapNotNull { it.getOrNull() }
                    .filterIsInstance<SerializedIncomingConnectionMessage>()
                    .take(transportMessages.size)
                    .toList()
            }

            storageManager.addPeers(peers)

            assertEquals(expected.sortedBy { it.content }, messages.await().sortedBy { it.content })
            verify(exactly = peers.size) {
                p2pClient.subscribeTo(
                    match { peersPublicKeys.containsLike(it.publicKey.asHexString().toByteArray()) }
                )
            }
        }
    }

    @Test
    fun `pairs with new peers`() {
        val peers = validNewPeers
        val peersPublicKeys = peers.map { it.publicKey.asHexString().toByteArray() }

        val (transportMessages, transportMessageFlows) = messagesAndFlows(peersPublicKeys)

        every { p2pClient.isSubscribed(any()) } returns false
        every { p2pClient.subscribeTo(any()) } answers { transportMessageFlows.getValue(firstArg<P2pPeer>().publicKey.asHexString().asString()) }

        runBlocking {
            val messages = async {
                p2pTransport.subscribe()
                    .onStart { transportMessageFlows.tryEmit(transportMessages) }
                    .take(transportMessages.size)
                    .collect()
            }

            storageManager.addPeers(peers)
            messages.await()

            val expectedInStorage = peers.map { it.copy(isPaired = true) }
            val fromStorage = async {
                storageManager.peers
                    .filter { it.isPaired }
                    .take(peers.size)
                    .toList()
            }

            println("1 size: ${peers.size}")
            assertEquals(expectedInStorage.sortedBy { it.name }, fromStorage.await().sortedBy { it.name })
            println("2")
            coVerify(exactly = peers.filter { !it.isPaired }.size) {
                p2pClient.sendPairingResponse(match { peers.contains(it) })
            }
            println("3")
        }
    }

    @Test
    fun `does not pair with known peers`() {
        val peers = validKnownPeers
        val peersPublicKeys = peers.map { it.publicKey.asHexString().toByteArray() }

        val (transportMessages, transportMessageFlows) = messagesAndFlows(peersPublicKeys)

        every { p2pClient.isSubscribed(any()) } returns false
        every { p2pClient.subscribeTo(any()) } answers { transportMessageFlows.getValue(firstArg<P2pPeer>().publicKey.asHexString().asString()) }

        runBlocking {
            storageManager.setPeers(peers)
            val messages = async {
                p2pTransport.subscribe()
                    .onStart { transportMessageFlows.tryEmit(transportMessages) }
                    .take(transportMessages.size)
                    .collect()
            }

            messages.await()

            val fromStorage = storageManager.getPeers()

            coVerify(exactly = 0) { p2pClient.sendPairingResponse(any()) }
            assertEquals(peers.sortedBy { it.name }, fromStorage.sortedBy { it.name })
        }
    }

    @Test
    fun `does not update new pairs if could not pair`() {
        val peers = validNewPeers
        val peersPublicKeys = peers.map { it.publicKey.asHexString().toByteArray() }

        val (transportMessages, transportMessageFlows) = messagesAndFlows(peersPublicKeys)

        every { p2pClient.isSubscribed(any()) } returns false
        coEvery { p2pClient.sendPairingResponse(any()) } returns Result.failure()
        every { p2pClient.subscribeTo(any()) } answers { transportMessageFlows.getValue(firstArg<P2pPeer>().publicKey.asHexString().asString()) }

        runBlocking {
            val messages = async {
                p2pTransport.subscribe()
                    .onStart { transportMessageFlows.tryEmit(transportMessages) }
                    .take(transportMessages.size)
                    .collect()
            }

            storageManager.addPeers(peers)
            messages.await()

            val expectedInStorage = peers.map { it.copy(isPaired = false) }
            val fromStorage = storageManager.getPeers()

            coVerify(exactly = peers.filter { !it.isPaired }.size) {
                p2pClient.sendPairingResponse(match { peers.contains(it) })
            }
            assertEquals(expectedInStorage.sortedBy { it.name }, fromStorage.sortedBy { it.name })
        }
    }

    @Test
    fun `does not subscribe to already subscribed peer`() {
        val (subscribedPeers, newPeers) = validKnownPeers.splitAt { it.size / 2 }
        val newPublicKeys = newPeers.map { it.publicKey.asHexString().toByteArray() }

        val (transportMessages, transportMessageFlows) = messagesAndFlows(newPublicKeys)

        every { p2pClient.isSubscribed(any()) } answers { subscribedPeers.contains(firstArg()) }
        every { p2pClient.subscribeTo(any()) } answers { transportMessageFlows.getValue(firstArg<P2pPeer>().publicKey.asHexString().asString()) }

        runBlocking {
            val messages = async {
                p2pTransport.subscribe()
                    .onStart { transportMessageFlows.tryEmit(transportMessages) }
                    .take(transportMessages.size)
                    .collect()
            }

            storageManager.addPeers(subscribedPeers + newPeers)

            messages.await()

            val fromStorage = async {
                storageManager.peers
                    .filter { it.isPaired }
                    .take(subscribedPeers.size + newPeers.size)
                    .toList()
            }

            fromStorage.await()

            coVerify(exactly = newPeers.size) { p2pClient.subscribeTo(match { newPublicKeys.containsLike(it.publicKey.asHexString().toByteArray()) }) }
            verify(exactly = subscribedPeers.size + newPeers.size) { p2pClient.isSubscribed(any()) }
        }
    }

    private val validKnownPeers = listOf(
        P2pPeer("1", "peer1", "0x00", "relayServer", isPaired = true),
        P2pPeer("2", "peer2", "0x01", "relayServer", "1", isPaired = true),
        P2pPeer("3", "peer3", "0x02", "relayServer", "2", isPaired = true),
        P2pPeer("4", "peer4", "0x03", "relayServer", "3", isPaired = true),
    )

    private val validNewPeers = listOf(
        P2pPeer("1", "peer1", "0x00", "relayServer", isPaired = false),
        P2pPeer("2", "peer2", "0x01", "relayServer", "1", isPaired = false),
        P2pPeer("3", "peer3", "0x02", "relayServer", "2", isPaired = false),
        P2pPeer("4", "peer4", "0x03", "relayServer", "3", isPaired = false),
    )

    private fun messagesAndFlows(
        publicKeys: List<ByteArray>,
    ): Pair<List<P2pMessage>, Map<String, MutableSharedFlow<Result<P2pMessage>>>> {
        val transportMessages = publicKeys.mapIndexed { index, bytes ->
            P2pMessage(bytes.toHexString().asString(), "content$index")
        }

        val transportMessageFlows = publicKeys.map {
            it.toHexString().asString() to MutableSharedFlow<Result<P2pMessage>>(transportMessages.size + 1)
        } .toMap()

        return Pair(transportMessages, transportMessageFlows)
    }

    private fun Map<String, MutableSharedFlow<Result<P2pMessage>>>.tryEmit(messages: List<P2pMessage>) {
        messages.forEach { getValue(it.publicKey).tryEmit(Result.success(it)) }
    }
}