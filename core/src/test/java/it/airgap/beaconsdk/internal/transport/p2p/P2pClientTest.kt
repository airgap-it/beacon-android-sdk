package it.airgap.beaconsdk.internal.transport.p2p

import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.data.beacon.P2pPeer
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.internal.crypto.data.SessionKeyPair
import it.airgap.beaconsdk.internal.transport.p2p.data.P2pMessage
import it.airgap.beaconsdk.internal.transport.p2p.matrix.MatrixClient
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixEvent
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixRoom
import it.airgap.beaconsdk.internal.transport.p2p.utils.P2pCommunicationUtils
import it.airgap.beaconsdk.internal.transport.p2p.utils.P2pServerUtils
import it.airgap.beaconsdk.internal.utils.HexString
import it.airgap.beaconsdk.internal.utils.Success
import it.airgap.beaconsdk.internal.utils.asHexString
import it.airgap.beaconsdk.internal.utils.encodeToHexString
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import mockLog
import onNth
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class P2pClientTest {

    @MockK
    private lateinit var p2pServerUtils: P2pServerUtils

    @MockK
    private lateinit var p2pCommunicationUtils: P2pCommunicationUtils

    @MockK(relaxUnitFun = true)
    private lateinit var matrixClient1: MatrixClient

    @MockK(relaxUnitFun = true)
    private lateinit var matrixClient2: MatrixClient

    @MockK
    private lateinit var crypto: Crypto

    private lateinit var p2pClient: P2pClient

    private val appName = "mockApp"
    private val appIcon = "mockAppIcon"
    private val appUrl = "mockAppUrl"
    private val keyPair = KeyPair(byteArrayOf(0), byteArrayOf(0))

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockLog()

        coEvery { matrixClient1.isLoggedIn() } returns true
        coEvery { matrixClient2.isLoggedIn() } returns true

        every { crypto.hash(any<String>(), any()) } answers {
            val arg = firstArg<String>()
            val bytes = HexString.fromStringOrNull(arg)?.asByteArray() ?: arg.encodeToByteArray()
            Success(bytes)
        }
        every {
            crypto.hash(any<HexString>(),
                any())
        } answers { Success(firstArg<HexString>().asByteArray()) }
        every { crypto.hash(any<ByteArray>(), any()) } answers { Success(firstArg()) }

        every { crypto.hashKey(any<HexString>()) } answers { Success(firstArg<HexString>().asByteArray()) }
        every { crypto.hashKey(any<ByteArray>()) } answers { Success(firstArg()) }

        every { crypto.createServerSessionKeyPair(any(), any()) } answers {
            Success(SessionKeyPair(byteArrayOf(0),
                byteArrayOf(0)))
        }
        every { crypto.createClientSessionKeyPair(any(), any()) } answers {
            Success(SessionKeyPair(byteArrayOf(0),
                byteArrayOf(0)))
        }

        every {
            crypto.signMessageDetached(any<String>(),
                any())
        } answers { Success(firstArg<String>().encodeToByteArray()) }
        every {
            crypto.signMessageDetached(any<HexString>(),
                any())
        } answers { Success(firstArg<HexString>().asByteArray()) }
        every {
            crypto.signMessageDetached(any<ByteArray>(),
                any())
        } answers { Success(firstArg()) }

        every { crypto.encryptMessageWithPublicKey(any<String>(), any()) } answers {
            Success(firstArg<String>().encodeToByteArray())
        }
        every { crypto.encryptMessageWithPublicKey(any<HexString>(), any()) } answers {
            Success(firstArg<HexString>().asByteArray())
        }
        every { crypto.encryptMessageWithPublicKey(any<ByteArray>(), any()) } answers {
            Success(firstArg())
        }

        every { crypto.encryptMessageWithSharedKey(any<String>(), any()) } answers {
            Success(firstArg<String>().encodeToByteArray())
        }
        every { crypto.encryptMessageWithSharedKey(any<HexString>(), any()) } answers {
            Success(firstArg<HexString>().asByteArray())
        }
        every { crypto.encryptMessageWithSharedKey(any<ByteArray>(), any()) } answers {
            Success(firstArg())
        }

        every { crypto.decryptMessageWithSharedKey(any<String>(), any()) } answers {
            Success(firstArg<String>().encodeToByteArray())
        }
        every { crypto.decryptMessageWithSharedKey(any<HexString>(), any()) } answers {
            Success(firstArg<HexString>().asByteArray())
        }
        every { crypto.decryptMessageWithSharedKey(any<ByteArray>(), any()) } answers {
            Success(firstArg())
        }

        p2pClient = P2pClient(
            appName,
            appIcon,
            appUrl,
            p2pServerUtils,
            p2pCommunicationUtils,
            listOf(matrixClient1, matrixClient2),
            crypto,
            keyPair
        )
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `subscribes for messages`() {
        val publicKey = HexString.fromString("0x00")

        val messages1 = listOf(
            validMatrixTextMessage("1", publicKey.asString(), "content1")
        )
        val messages2 = listOf(
            validMatrixTextMessage("1", publicKey.asString(), "content2")
        )

        every { p2pCommunicationUtils.isMessageFrom(any(), any()) } returns true
        every { crypto.validateEncryptedMessage(any()) } returns true
        every { matrixClient1.events } returns flow<MatrixEvent> {
            messages1.forEach { emit(it) }
        }
        every { matrixClient2.events } returns flow<MatrixEvent> {
            messages2.forEach { emit(it) }
        }

        runBlocking {
            val expected =
                (messages1 + messages2).map { P2pMessage(publicKey.asString(), it.message) }
            val messages = p2pClient.subscribeTo(publicKey).mapNotNull { it.getOrNull() }.toList()

            assertEquals(expected.sortedBy { it.content }, messages.sortedBy { it.content })
        }
    }

    @Test
    fun `checks if peer is already subscribed`() {
        every { matrixClient1.events } returns flow { }
        every { matrixClient2.events } returns flow { }

        val subscribed = HexString.fromString("0x00").also { p2pClient.subscribeTo(it) }
        val unsubscribed = HexString.fromString("0x01")

        assertTrue(p2pClient.isSubscribed(subscribed),
            "Expected peer to be recognized as subscribed")
        assertFalse(p2pClient.isSubscribed(unsubscribed),
            "Expected peer to be recognized as subscribed")
    }

    @Test
    fun `ignores text messages from unsubscribed senders`() {
        val subscribedPublicKey = HexString.fromString("0x00")
        val unsubscribedPublicKey = HexString.fromString("0x01")

        val subscribedMessages = listOf(
            validMatrixTextMessage("1", subscribedPublicKey.asString(), "content1")
        )
        val unsubscribedMessages = listOf(
            validMatrixTextMessage("1", unsubscribedPublicKey.asString(), "content2")
        )

        every {
            p2pCommunicationUtils.isMessageFrom(match { subscribedMessages.contains(it) },
                any())
        } returns true
        every {
            p2pCommunicationUtils.isMessageFrom(match { unsubscribedMessages.contains(it) },
                any())
        } returns false
        every { crypto.validateEncryptedMessage(any()) } returns true
        every { matrixClient1.events } returns flow<MatrixEvent> {
            subscribedMessages.forEach { emit(it) }
        }
        every { matrixClient2.events } returns flow<MatrixEvent> {
            unsubscribedMessages.forEach { emit(it) }
        }

        runBlocking {
            val expected =
                subscribedMessages.map { P2pMessage(subscribedPublicKey.asString(), it.message) }
            val messages = p2pClient.subscribeTo(subscribedPublicKey)
                .mapNotNull { it.getOrNull() }.toList()

            assertEquals(expected.sortedBy { it.content }, messages.sortedBy { it.content })
        }
    }

    @Test
    fun `ignores invalid text messages`() {
        val publicKey = HexString.fromString("0x00")

        val validMessages = listOf(
            validMatrixTextMessage("1", publicKey.asString(), "valid1")
        )
        val invalidMessages = listOf(
            validMatrixTextMessage("1", publicKey.asString(), "invalid2")
        )

        every { p2pCommunicationUtils.isMessageFrom(any(), any()) } returns true
        every { crypto.validateEncryptedMessage(match { it.startsWith("valid") }) } returns true
        every { crypto.validateEncryptedMessage(match { it.startsWith("invalid") }) } returns false

        every { matrixClient1.events } returns flow<MatrixEvent> {
            validMessages.forEach { emit(it) }
        }
        every { matrixClient2.events } returns flow<MatrixEvent> {
            invalidMessages.forEach { emit(it) }
        }

        runBlocking {
            val expected = validMessages.map { P2pMessage(publicKey.asString(), it.message) }
            val messages = p2pClient.subscribeTo(publicKey).mapNotNull { it.getOrNull() }.toList()

            assertEquals(expected.sortedBy { it.content }, messages.sortedBy { it.content })
        }
    }

    @Test
    fun `ignores events other than text messages on subscribe`() {
        val publicKey = HexString.fromString("0x00")

        val textMessages1 = listOf(
            validMatrixTextMessage("1", publicKey.asString(), "content1")
        )
        val otherMessages1 = listOf(
            validMatrixInviteMessage("2")
        )

        val testMessages2 = listOf(
            validMatrixTextMessage("1", publicKey.asString(), "content2")
        )
        val otherMessages2 = listOf(
            validMatrixInviteMessage("2")
        )

        every { p2pCommunicationUtils.isMessageFrom(any(), any()) } returns true
        every { crypto.validateEncryptedMessage(any()) } returns true
        every { matrixClient1.events } returns flow {
            textMessages1.forEach { emit(it) }
            otherMessages1.forEach { emit(it) }
        }
        every { matrixClient2.events } returns flow {
            testMessages2.forEach { emit(it) }
            otherMessages2.forEach { emit(it) }
        }

        runBlocking {
            val expected =
                (textMessages1 + testMessages2).map { P2pMessage(publicKey.asString(), it.message) }
            val messages = p2pClient.subscribeTo(publicKey).mapNotNull { it.getOrNull() }.toList()

            assertEquals(expected.sortedBy { it.content }, messages.sortedBy { it.content })
        }
    }

    @Test
    fun `unsubscribes from peer`() {
        val publicKey = HexString.fromString("0x00")

        every { p2pCommunicationUtils.isMessageFrom(any(), any()) } returns true
        every { crypto.validateEncryptedMessage(any()) } returns true

        val messages1 = listOf(
            validMatrixTextMessage("1", publicKey.asString(), "content1"),
            validMatrixTextMessage("1", publicKey.asString(), "content2"),
        )
        val messages2 = listOf(
            validMatrixTextMessage("2", publicKey.asString(), "content1"),
            validMatrixTextMessage("2", publicKey.asString(), "content2"),
        )

        val testDeferred = CompletableDeferred<Unit>()

        every { matrixClient1.events } returns flow<MatrixEvent> {
            messages1.forEach { emit(it) }
        }
        every { matrixClient2.events } returns flow<MatrixEvent> {
            testDeferred.await()
            p2pClient.unsubscribeFrom(publicKey)
            messages2.forEach { emit(it) }
        }

        val actual = runBlocking {
            p2pClient.subscribeTo(publicKey)
                .mapNotNull { it.getOrNull() }
                .onNth(messages1.size) { testDeferred.complete(Unit) }
                .toList()
        }

        val expected = messages1.map { P2pMessage(publicKey.asString(), it.message) }

        assertFalse(p2pClient.isSubscribed(publicKey),
            "Expected peer to be recognized as unsubscribed")
        assertEquals(expected.sortedBy { it.toString() }, actual.sortedBy { it.toString() })
    }

    @Test
    fun `does not continue previous flows if resubscribed`() {
        val publicKey = HexString.fromString("0x00")

        every { p2pCommunicationUtils.isMessageFrom(any(), any()) } returns true
        every { crypto.validateEncryptedMessage(any()) } returns true

        val messages1 = listOf(
            validMatrixTextMessage("1", publicKey.asString(), "content1"),
            validMatrixTextMessage("1", publicKey.asString(), "content2"),
        )
        val messages2 = listOf(
            validMatrixTextMessage("2", publicKey.asString(), "content1"),
            validMatrixTextMessage("2", publicKey.asString(), "content2"),
        )

        every { matrixClient1.events } returns flow<MatrixEvent> {
            messages1.forEach { emit(it) }
        }
        every { matrixClient2.events } returns flow<MatrixEvent> {
            messages2.forEach { emit(it) }
        }

        runBlocking {
            val unsubscribed = p2pClient.subscribeTo(publicKey).let {
                async { it.mapNotNull { it.getOrNull() }.toList() }
            }

            p2pClient.unsubscribeFrom(publicKey)
            p2pClient.subscribeTo(publicKey)

            println(unsubscribed.await())

            assertTrue(p2pClient.isSubscribed(publicKey),
                "Expected peer to be recognized as subscribed")
            assertTrue(unsubscribed.await().isEmpty(),
                "Expected immediately unsubscribed flow to be empty")
        }
    }

    @Test
    fun `sends message to specified recipient`() {
        every { p2pServerUtils.getRelayServer(any<ByteArray>(), any()) } returns ""
        every { p2pServerUtils.getRelayServer(any<HexString>(), any()) } returns ""

        every {
            p2pCommunicationUtils.recipientIdentifier(any(),
                any())
        } answers { firstArg<HexString>().asString() }

        coEvery { matrixClient1.sendTextMessage(any<MatrixRoom>(), any()) } returns Success()
        coEvery { matrixClient2.sendTextMessage(any<MatrixRoom>(), any()) } returns Success()

        val publicKey = HexString.fromString("0x00")
        val otherPublicKey = HexString.fromString("0x01")
        val relayServer = "relay_server"
        val peer =
            P2pPeer(name = "name", publicKey = publicKey.asString(), relayServer = relayServer)
        val message = "message"

        val room1 = MatrixRoom.Joined("room1", listOf(publicKey.asString()))
        val room2 = MatrixRoom.Joined("room2", listOf(otherPublicKey.asString()))

        coEvery { matrixClient1.joinedRooms() } returns listOf(room1, room2).shuffled()
        coEvery { matrixClient2.joinedRooms() } returns listOf(room1, room2).shuffled()

        runBlocking { p2pClient.sendTo(peer, message) }

        val expectedMessage = message.encodeToHexString().asString()

        coVerify(exactly = 1) { matrixClient1.sendTextMessage(room1, expectedMessage) }
        coVerify(exactly = 1) { matrixClient2.sendTextMessage(room1, expectedMessage) }
    }

    @Test
    fun `sends pairing request to matrix clients`() {
        every { p2pCommunicationUtils.recipientIdentifier(any(), any()) } answers {
            "${firstArg<HexString>().asString()}:${secondArg<String>()}"
        }

        val pairingPayload = "pairingPayload"
        every {
            p2pCommunicationUtils.pairingPayload(any(),
                any(),
                any(),
                any(),
                any(),
                any())
        } returns Success(pairingPayload)

        every { p2pCommunicationUtils.channelOpeningMessage(any(), any()) } answers {
            "@channel-open:${firstArg<String>()}:${secondArg<String>()}"
        }

        coEvery { matrixClient1.joinedRooms() } returns emptyList()
        coEvery { matrixClient2.joinedRooms() } returns emptyList()

        coEvery { matrixClient1.sendTextMessage(any<MatrixRoom>(), any()) } returns Success()
        coEvery { matrixClient2.sendTextMessage(any<MatrixRoom>(), any()) } returns Success()

        val publicKey = HexString.fromString("0x00")
        val relayServer = "relay_server"
        val clientRelayServer = "client_relay_server"
        val peer =
            P2pPeer(name = "name", publicKey = publicKey.asString(), relayServer = relayServer)

        val expectedRecipient = "${publicKey.asString()}:$relayServer"
        val expectedPayload = pairingPayload.encodeToByteArray().asHexString().asString()
        val expectedMessage = "@channel-open:$expectedRecipient:$expectedPayload"

        val newRoom = MatrixRoom.Unknown("room1", listOf(expectedRecipient))

        coEvery { matrixClient1.createTrustedPrivateRoom(any()) } returns Success(newRoom)
        coEvery { matrixClient2.createTrustedPrivateRoom(any()) } returns Success(newRoom)

        every { p2pServerUtils.getRelayServer(any<ByteArray>(), any()) } returns clientRelayServer
        every { p2pServerUtils.getRelayServer(any<HexString>(), any()) } returns clientRelayServer

        runBlocking { p2pClient.sendPairingResponse(peer) }

        coVerify(exactly = 1) { matrixClient1.sendTextMessage(newRoom, expectedMessage) }
        coVerify(exactly = 1) { matrixClient2.sendTextMessage(newRoom, expectedMessage) }
    }

    private fun validMatrixTextMessage(
        roomId: String,
        senderHash: String,
        content: String,
    ): MatrixEvent.TextMessage =
        MatrixEvent.TextMessage(roomId, "@$senderHash", content)

    private fun validMatrixInviteMessage(roomId: String): MatrixEvent.Invite =
        MatrixEvent.Invite(roomId)

}