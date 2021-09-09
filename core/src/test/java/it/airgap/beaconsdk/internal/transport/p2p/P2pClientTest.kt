package it.airgap.beaconsdk.internal.transport.p2p

import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.data.beacon.P2pPeer
import it.airgap.beaconsdk.internal.BeaconConfiguration
import it.airgap.beaconsdk.internal.transport.p2p.data.P2pIdentifier
import it.airgap.beaconsdk.internal.transport.p2p.data.P2pMessage
import it.airgap.beaconsdk.internal.transport.p2p.matrix.MatrixClient
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixEvent
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixRoom
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.MatrixError
import it.airgap.beaconsdk.internal.transport.p2p.store.*
import it.airgap.beaconsdk.internal.transport.p2p.utils.P2pCommunicator
import it.airgap.beaconsdk.internal.transport.p2p.utils.P2pCrypto
import it.airgap.beaconsdk.internal.utils.asHexString
import it.airgap.beaconsdk.internal.utils.failure
import it.airgap.beaconsdk.internal.utils.success
import it.airgap.beaconsdk.internal.utils.toHexString
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import mockLog
import onNth
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class P2pClientTest {

    @MockK
    private lateinit var matrixClient: MatrixClient

    @MockK
    private lateinit var p2pStore: P2pStore

    @MockK
    private lateinit var p2pCrypto: P2pCrypto

    @MockK
    private lateinit var p2pProtocol: P2pCommunicator

    private lateinit var p2pClient: P2pClient

    private val userId: String = "userId"
    private val password: String = "password"
    private val deviceId: String = "deviceId"

    private lateinit var testDeferred: CompletableDeferred<Unit>

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockLog()

        coEvery { matrixClient.isLoggedIn() } returns true
        coEvery { matrixClient.start(any(), any(), any(), any()) } returns Result.success()
        coJustRun { matrixClient.resetHard(any()) }

        coEvery { p2pStore.intent(any()) } returns Result.success()

        coEvery { p2pCrypto.userId() } returns Result.success(userId)
        coEvery { p2pCrypto.password() } returns Result.success(password)
        coEvery { p2pCrypto.deviceId() } returns Result.success(deviceId)

        coEvery { p2pCrypto.encrypt(any(), any()) } answers { Result.success(secondArg()) }
        coEvery { p2pCrypto.decrypt(any(), any()) } answers { Result.success(secondArg()) }

        coEvery { p2pCrypto.encryptPairingPayload(any(), any()) } answers { Result.success(secondArg<String>().encodeToByteArray()) }

        every { p2pProtocol.recipientIdentifier(any(), any()) } answers {
            Result.success(P2pIdentifier(firstArg(), secondArg()))
        }

        coEvery { matrixClient.stop(any()) } returns Unit

        testDeferred = CompletableDeferred()

        p2pClient = P2pClient(matrixClient, p2pStore, p2pCrypto, p2pProtocol)
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `subscribes for messages`() {
        val publicKey = "0x00".asHexString().toByteArray()
        val relayServer = "relayServer"
        val roomId = "1"

        val messages = listOf(
            validMatrixTextMessage(relayServer, roomId, publicKey.toHexString().asString(), "content1")
        )

        every { p2pProtocol.isMessageFrom(any(), any()) } returns true
        every { p2pProtocol.isValidMessage(any()) } returns true
        every { matrixClient.events } returns flow {
            messages.forEach { emit(it) }
        }

        coEvery { p2pStore.state() } answers {
            Result.success(
                P2pStoreState(
                    relayServer = relayServer,
                    availableNodes = 1,
                )
            )
        }

        runBlocking {
            val expected = messages.map { P2pMessage(publicKey.toHexString().asString(), it.message) }
            val actual = p2pClient.subscribeTo(publicKey).mapNotNull { it.getOrNull() }.toList()

            assertEquals(expected.sortedBy { it.content }, actual.sortedBy { it.content })
            coVerify(exactly = 1) { p2pStore.intent(OnChannelEvent(publicKey.toHexString().asString(), roomId)) }
        }
    }

    @Test
    fun `starts Matrix client if not run`() {
        val publicKey = "0x00".asHexString().toByteArray()
        val relayServer = "relayServer"

        every { p2pProtocol.isMessageFrom(any(), any()) } returns true
        every { p2pProtocol.isValidMessage(any()) } returns true

        coEvery { matrixClient.isLoggedIn() } returns false andThen true
        coEvery { matrixClient.start(any(), any(), any(), any()) } answers {
            testDeferred.complete(Unit)
            Result.success()
        }
        every { matrixClient.events } returns flow { testDeferred.await() }

        coEvery { p2pStore.state() } answers {
            Result.success(
                P2pStoreState(
                    relayServer = relayServer,
                    availableNodes = 1,
                )
            )
        }

        runBlocking {
            withTimeoutOrNull(1000) { p2pClient.subscribeTo(publicKey).collect() }

            coVerify(exactly = 1) { matrixClient.start(relayServer, userId, password, deviceId) }
        }
    }

    @Test
    fun `tries to star Matrix client and resets state on every failure`() {
        val publicKey = "0x00".asHexString().toByteArray()
        val relayServer = "relayServer"

        every { p2pProtocol.isMessageFrom(any(), any()) } returns true
        every { p2pProtocol.isValidMessage(any()) } returns true

        coEvery { matrixClient.isLoggedIn() } returns false andThen true
        coEvery { matrixClient.start(any(), any(), any(), any()) } answers {
            Result.failure()
        } andThenAnswer {
            Result.failure()
        } andThenAnswer {
            testDeferred.complete(Unit)
            Result.success()
        }
        every { matrixClient.events } returns flow { testDeferred.await() }

        coEvery { p2pStore.state() } answers {
            Result.success(
                P2pStoreState(
                    relayServer = relayServer,
                    availableNodes = 3,
                )
            )
        }

        runBlocking {
            withTimeoutOrNull(1000) { p2pClient.subscribeTo(publicKey).collect() }

            coVerify(exactly = 3) { matrixClient.start(relayServer, userId, password, deviceId) }
            coVerify(exactly = 2) { matrixClient.resetHard() }
            coVerify(exactly = 2) { p2pStore.intent(HardReset) }
        }
    }

    @Test
    fun `checks if peer is already subscribed`() {
        every { matrixClient.events } returns flow { }

        val subscribed = "0x00".asHexString().toByteArray().also { p2pClient.subscribeTo(it) }
        val unsubscribed = "0x01".asHexString().toByteArray()

        assertTrue(p2pClient.isSubscribed(subscribed),
            "Expected peer to be recognized as subscribed")
        assertFalse(p2pClient.isSubscribed(unsubscribed),
            "Expected peer to be recognized as subscribed")
    }

    @Test
    fun `ignores text messages from unsubscribed senders`() {
        val subscribedPublicKey = "0x00".asHexString().toByteArray()
        val unsubscribedPublicKey = "0x01".asHexString().toByteArray()

        val relayServer = "relayServer"

        val subscribedMessages = listOf(
            validMatrixTextMessage(relayServer, "1", subscribedPublicKey.toHexString().asString(), "content1")
        )
        val unsubscribedMessages = listOf(
            validMatrixTextMessage(relayServer, "1", unsubscribedPublicKey.toHexString().asString(), "content2")
        )

        every {
            p2pProtocol.isMessageFrom(match { subscribedMessages.contains(it) },
                any())
        } returns true
        every {
            p2pProtocol.isMessageFrom(match { unsubscribedMessages.contains(it) },
                any())
        } returns false
        every { p2pProtocol.isValidMessage(any()) } returns true
        every { matrixClient.events } returns flow {
            subscribedMessages.forEach { emit(it) }
            unsubscribedMessages.forEach { emit(it) }
        }

        coEvery { p2pStore.state() } answers {
            Result.success(
                P2pStoreState(
                    relayServer = relayServer,
                    availableNodes = 1,
                )
            )
        }

        runBlocking {
            val expected = subscribedMessages.map { P2pMessage(subscribedPublicKey.toHexString().asString(), it.message) }
            val actual = p2pClient.subscribeTo(subscribedPublicKey).mapNotNull { it.getOrNull() }.toList()

            assertEquals(expected.sortedBy { it.content }, actual.sortedBy { it.content })
        }
    }

    @Test
    fun `ignores invalid text messages`() {
        val publicKey = "0x00".asHexString().toByteArray()
        val relayServer = "relayServer"

        val validMessages = listOf(
            validMatrixTextMessage(relayServer, "1", publicKey.toHexString().asString(), "valid1")
        )
        val invalidMessages = listOf(
            validMatrixTextMessage(relayServer, "1", publicKey.toHexString().asString(), "invalid2")
        )

        every { p2pProtocol.isMessageFrom(any(), any()) } returns true
        every { p2pProtocol.isValidMessage(match { it.message.startsWith("valid") }) } returns true
        every { p2pProtocol.isValidMessage(match { it.message.startsWith("invalid") }) } returns false

        every { matrixClient.events } returns flow {
            validMessages.forEach { emit(it) }
            invalidMessages.forEach { emit(it) }
        }

        coEvery { p2pStore.state() } answers {
            Result.success(
                P2pStoreState(
                    relayServer = relayServer,
                    availableNodes = 1,
                )
            )
        }

        runBlocking {
            val expected = validMessages.map { P2pMessage(publicKey.toHexString().asString(), it.message) }
            val actual = p2pClient.subscribeTo(publicKey).mapNotNull { it.getOrNull() }.toList()

            assertEquals(expected.sortedBy { it.content }, actual.sortedBy { it.content })
        }
    }

    @Test
    fun `ignores events other than text messages on subscribe`() {
        val publicKey = "0x00".asHexString().toByteArray()
        val relayServer = "relayServer"

        val textMessages1 = listOf(
            validMatrixTextMessage(relayServer, "1", publicKey.toHexString().asString(), "content1")
        )
        val otherMessages1 = listOf(
            validMatrixInviteMessage(relayServer, "2", publicKey.toHexString().asString())
        )

        val textMessages2 = listOf(
            validMatrixTextMessage(relayServer, "1", publicKey.toHexString().asString(), "content2")
        )
        val otherMessages2 = listOf(
            validMatrixInviteMessage(relayServer, "2", publicKey.toHexString().asString())
        )

        every { p2pProtocol.isMessageFrom(any(), any()) } returns true
        every { p2pProtocol.isValidMessage(any()) } returns true
        every { matrixClient.events } returns flow {
            textMessages1.forEach { emit(it) }
            otherMessages1.forEach { emit(it) }
            textMessages2.forEach { emit(it) }
            otherMessages2.forEach { emit(it) }
        }

        coEvery { p2pStore.state() } answers {
            Result.success(
                P2pStoreState(
                    relayServer = relayServer,
                    availableNodes = 1,
                )
            )
        }

        runBlocking {
            val expected = (textMessages1 + textMessages2).map { P2pMessage(publicKey.toHexString().asString(), it.message) }
            val actual = p2pClient.subscribeTo(publicKey).mapNotNull { it.getOrNull() }.toList()

            assertEquals(expected.sortedBy { it.content }, actual.sortedBy { it.content })
        }
    }

    @Test
    fun `ignores events coming from nodes other than active`() {
        val publicKey = "0x00".asHexString().toByteArray()
        val relayServer = "relayServer"
        val otherRelayServer = "otherRelayServer"

        val textMessages = listOf(
            validMatrixTextMessage(relayServer, "1", publicKey.toHexString().asString(), "content1")
        )
        val otherTextMessages = listOf(
            validMatrixTextMessage(otherRelayServer, "1", publicKey.toHexString().asString(), "content2")
        )

        every { p2pProtocol.isMessageFrom(any(), any()) } returns true
        every { p2pProtocol.isValidMessage(any()) } returns true
        every { matrixClient.events } returns flow {
            textMessages.forEach { emit(it) }
            otherTextMessages.forEach { emit(it) }
        }

        coEvery { p2pStore.state() } answers {
            Result.success(
                P2pStoreState(
                    relayServer = relayServer,
                    availableNodes = 1,
                )
            )
        }

        runBlocking {
            val expected = textMessages.map { P2pMessage(publicKey.toHexString().asString(), it.message) }
            val actual = p2pClient.subscribeTo(publicKey).mapNotNull { it.getOrNull() }.toList()

            assertEquals(expected.sortedBy { it.content }, actual.sortedBy { it.content })
        }
    }

    @Test
    fun `does not ignore events coming from any nodes if there is no active`() {
        val publicKey = "0x00".asHexString().toByteArray()
        val relayServer = "relayServer"
        val otherRelayServer = "otherRelayServer"

        val textMessages = listOf(
            validMatrixTextMessage(relayServer, "1", publicKey.toHexString().asString(), "content1")
        )
        val otherTextMessages = listOf(
            validMatrixTextMessage(otherRelayServer, "1", publicKey.toHexString().asString(), "content2")
        )

        every { p2pProtocol.isMessageFrom(any(), any()) } returns true
        every { p2pProtocol.isValidMessage(any()) } returns true
        every { matrixClient.events } returns flow {
            textMessages.forEach { emit(it) }
            otherTextMessages.forEach { emit(it) }
        }

        coEvery { p2pStore.state() } returns Result.failure()

        runBlocking {
            val expected = (textMessages + otherTextMessages).map { P2pMessage(publicKey.toHexString().asString(), it.message) }
            val actual = p2pClient.subscribeTo(publicKey).mapNotNull { it.getOrNull() }.toList()

            assertEquals(expected.sortedBy { it.content }, actual.sortedBy { it.content })
        }
    }

    @Test
    fun `unsubscribes from peer`() {
        val publicKey = "0x00".asHexString().toByteArray()
        val relayServer = "relayServer"

        every { p2pProtocol.isMessageFrom(any(), any()) } returns true
        every { p2pProtocol.isValidMessage(any()) } returns true

        val messages1 = listOf(
            validMatrixTextMessage(relayServer, "1", publicKey.toHexString().asString(), "content1"),
            validMatrixTextMessage(relayServer, "1", publicKey.toHexString().asString(), "content2"),
        )
        val messages2 = listOf(
            validMatrixTextMessage(relayServer, "2", publicKey.toHexString().asString(), "content1"),
            validMatrixTextMessage(relayServer, "2", publicKey.toHexString().asString(), "content2"),
        )

        val testDeferred = CompletableDeferred<Unit>()

        every { matrixClient.events } returns flow {
            messages1.forEach { emit(it) }
            testDeferred.await()
            p2pClient.unsubscribeFrom(publicKey)
            messages2.forEach { emit(it) }
        }

        coEvery { p2pStore.state() } answers {
            Result.success(
                P2pStoreState(
                    relayServer = relayServer,
                    availableNodes = 1,
                )
            )
        }

        val actual = runBlocking {
            p2pClient.subscribeTo(publicKey)
                .mapNotNull { it.getOrNull() }
                .onNth(messages1.size) { testDeferred.complete(Unit) }
                .toList()
        }

        val expected = messages1.map { P2pMessage(publicKey.toHexString().asString(), it.message) }

        assertFalse(p2pClient.isSubscribed(publicKey),
            "Expected peer to be recognized as unsubscribed")
        assertEquals(expected.sortedBy { it.toString() }, actual.sortedBy { it.toString() })
    }

    @Test
    fun `stops Matrix client if no active subscriptions`() {
        val publicKey = "0x00".asHexString().toByteArray()

        runBlocking {
            p2pClient.subscribeTo(publicKey)
            p2pClient.unsubscribeFrom(publicKey)

            coVerify(exactly = 1) { matrixClient.stop() }
        }
    }

    @Test
    fun `does not continue previous flows if resubscribed`() {
        val publicKey = "0x00".asHexString().toByteArray()
        val relayServer = "relayServer"

        every { p2pProtocol.isMessageFrom(any(), any()) } returns true
        every { p2pProtocol.isValidMessage(any()) } returns true

        val messages = listOf(
            validMatrixTextMessage(relayServer, "1", publicKey.toHexString().asString(), "content1"),
            validMatrixTextMessage(relayServer, "1", publicKey.toHexString().asString(), "content2"),
        )

        every { matrixClient.events } returns flow {
            messages.forEach { emit(it) }
        }

        coEvery { p2pStore.state() } answers {
            Result.success(
                P2pStoreState(
                    relayServer = relayServer,
                    availableNodes = 1,
                )
            )
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
    fun `sends message to specified recipient with cached active channel`() {
        coEvery { matrixClient.sendTextMessage(any(), any<String>(), any()) } returns Result.success()

        val publicKey = "0x00".asHexString().toByteArray()
        val relayServer = "relayServer"
        val identifier = P2pIdentifier(publicKey, relayServer)
        val peer = P2pPeer(name = "name", publicKey = publicKey.toHexString().asString(), relayServer = relayServer)
        val message = "message"
        val room = MatrixRoom.Unknown("room1", listOf(identifier.asString()))

        coEvery { p2pStore.state() } returns (
            Result.success(
                P2pStoreState(
                    relayServer = relayServer,
                    availableNodes = 1,
                    activeChannels = mapOf(
                        identifier.asString() to room.id,
                    ),
                )
            )
        )

        runBlocking { p2pClient.sendTo(peer, message) }

        coVerify(exactly = 1) { matrixClient.sendTextMessage(relayServer, room.id, message) }
    }

    @Test
    fun `sends message to specified recipient without cached active channel`() {
        coEvery { matrixClient.sendTextMessage(any(), any<String>(), any()) } returns Result.success()

        val publicKey = "0x00".asHexString().toByteArray()
        val relayServer = "relayServer"
        val identifier = P2pIdentifier(publicKey, relayServer)
        val peer = P2pPeer(name = "name", publicKey = publicKey.toHexString().asString(), relayServer = relayServer)
        val message = "message"
        val room = MatrixRoom.Joined("room1", listOf(identifier.asString()))

        coEvery { matrixClient.joinedRooms() } returns listOf(room)

        coEvery { p2pStore.state() } returns (
            Result.success(
                P2pStoreState(
                    relayServer = relayServer,
                    availableNodes = 1,
                    activeChannels = emptyMap(),
                )
            )
        )

        runBlocking { p2pClient.sendTo(peer, message) }

        coVerify(exactly = 1) { matrixClient.sendTextMessage(relayServer, room.id, message) }
    }

    @Test
    fun `sends message to specified recipient and retries on denied access`() {
        coEvery { matrixClient.sendTextMessage(any(), any<String>(), any()) } answers {
            Result.failure(MatrixError.Forbidden(""))
        } andThenAnswer {
            Result.success()
        }

        val publicKey = "0x00".asHexString().toByteArray()
        val relayServer = "relayServer"
        val identifier = P2pIdentifier(publicKey, relayServer)
        val peer = P2pPeer(name = "name", publicKey = publicKey.toHexString().asString(), relayServer = relayServer)
        val message = "message"
        val invalidRoom = MatrixRoom.Joined("invalidRoom", listOf(identifier.asString()))
        val validRoom = MatrixRoom.Joined("validRoom", listOf(identifier.asString()))

        coEvery { matrixClient.joinedRooms() } returns listOf(invalidRoom, validRoom)

        coEvery { p2pStore.state() } returns (
            Result.success(
                P2pStoreState(
                    relayServer = relayServer,
                    availableNodes = 1,
                    activeChannels = mapOf(
                        identifier.asString() to invalidRoom.id,
                    ),
                )
            )
        ) andThen (
            Result.success(
                P2pStoreState(
                    relayServer = relayServer,
                    availableNodes = 1,
                    activeChannels = emptyMap(),
                    inactiveChannels = setOf(invalidRoom.id),
                )
            )
        )

        runBlocking { p2pClient.sendTo(peer, message) }

        coVerify(exactly = 1) { p2pStore.intent(OnChannelClosed(invalidRoom.id)) }
        coVerify(exactly = 1) { matrixClient.sendTextMessage(relayServer, invalidRoom.id, message) }
        coVerify(exactly = 1) { matrixClient.sendTextMessage(relayServer, validRoom.id, message) }
    }

    @Test
    fun `sends message to specified recipient and fails on error other than access denied`() {
        coEvery { matrixClient.sendTextMessage(any(), any<String>(), any()) } answers {
            Result.failure(IOException())
        } andThenAnswer {
            Result.success()
        }

        val publicKey = "0x00".asHexString().toByteArray()
        val relayServer = "relayServer"
        val identifier = P2pIdentifier(publicKey, relayServer)
        val peer = P2pPeer(name = "name", publicKey = publicKey.toHexString().asString(), relayServer = relayServer)
        val message = "message"
        val invalidRoom = MatrixRoom.Joined("invalidRoom", listOf(identifier.asString()))
        val validRoom = MatrixRoom.Joined("validRoom", listOf(identifier.asString()))

        coEvery { matrixClient.joinedRooms() } returns listOf(invalidRoom, validRoom)

        coEvery { p2pStore.state() } returns (
            Result.success(
                P2pStoreState(
                    relayServer = relayServer,
                    availableNodes = 1,
                    activeChannels = mapOf(
                        identifier.asString() to invalidRoom.id,
                    ),
                )
            )
        )

        runBlocking {
            assertTrue(p2pClient.sendTo(peer, message).isFailure)

            coVerify(exactly = 1) { matrixClient.sendTextMessage(relayServer, invalidRoom.id, message) }
        }
    }

    @Test
    fun `sends pairing request to matrix clients`() {
        val pairingPayload = "pairingPayload"
        every { p2pProtocol.pairingPayload(any(), any()) } returns Result.success(pairingPayload)
        every { p2pProtocol.channelOpeningMessage(any(), any()) } answers {
            "@channel-open:${firstArg<String>()}:${secondArg<String>()}"
        }

        coEvery { matrixClient.sendTextMessage(any(), any<String>(), any()) } returns Result.success()
        coEvery { matrixClient.sendTextMessage(any(), any<MatrixRoom>(), any()) } returns Result.success()

        val publicKey = "0x00".asHexString().toByteArray()
        val relayServer = "relayServer"
        val clientRelayServer = "clientRelayServer"
        val peer = P2pPeer(name = "name", publicKey = publicKey.toHexString().asString(), relayServer = relayServer)

        val expectedRecipient = P2pIdentifier(publicKey, relayServer)
        val expectedPayload = pairingPayload.encodeToByteArray().toHexString().asString()
        val expectedMessage = "@channel-open:${expectedRecipient.asString()}:$expectedPayload"

        coEvery { p2pStore.state() } returns (
            Result.success(
                P2pStoreState(
                    relayServer = clientRelayServer,
                    availableNodes = 1,
                )
            )
        )

        val newRoom = MatrixRoom.Unknown("room1", listOf(expectedRecipient.asString()))

        coEvery { matrixClient.createTrustedPrivateRoom(any(), any()) } returns Result.success(newRoom)

        runBlocking { p2pClient.sendPairingResponse(peer) }

        coVerify(exactly = 1) { matrixClient.sendTextMessage(clientRelayServer, newRoom.id, expectedMessage) }
        coVerify(exactly = 1) { p2pStore.intent(OnChannelCreated(expectedRecipient.asString(), newRoom.id)) }
    }

    @Test
    fun `joins room on invite event`() {
        val publicKey = "0x00".asHexString().toByteArray()
        val relayServer = "relayServer"
        val roomId = "1"

        val inviteMessages = listOf(
            validMatrixInviteMessage(relayServer, roomId, publicKey.toHexString().asString())
        )

        coEvery { p2pStore.state() } returns Result.success(
            P2pStoreState(
                relayServer = relayServer,
                availableNodes = 1,
            )
        )

        coEvery { matrixClient.isLoggedIn() } returns false andThen true
        every { matrixClient.events } returns flow {
            inviteMessages.forEach { emit(it) }
            testDeferred.await()
        }
        coEvery { matrixClient.joinRoom(any(), any<String>()) } answers {
            testDeferred.complete(Unit)
            Result.success()
        }

        runBlocking {
            withTimeoutOrNull(10000) { p2pClient.subscribeTo(publicKey).collect() }

            coVerify(exactly = inviteMessages.distinctBy { it.roomId }.size) {
                p2pStore.intent(OnChannelEvent(publicKey.toHexString().asString(), roomId))
            }
            coVerify(exactly = 1) { matrixClient.joinRoom(relayServer, roomId) }
        }
    }

    @Test
    fun `retries to join room on invite event if denied access`() {
        val publicKey = "0x00".asHexString().toByteArray()
        val relayServer = "relayServer"
        val roomId = "1"

        val inviteMessages = listOf(
            validMatrixInviteMessage(relayServer, roomId, publicKey.toHexString().asString())
        )

        coEvery { p2pStore.state() } returns Result.success(
            P2pStoreState(
                relayServer = relayServer,
                availableNodes = 1,
            )
        )

        coEvery { matrixClient.isLoggedIn() } returns false andThen true
        every { matrixClient.events } returns flow {
            inviteMessages.forEach { emit(it) }
            testDeferred.await()
            delay(BeaconConfiguration.P2P_JOIN_DELAY_MS * 2)
        }
        coEvery { matrixClient.joinRoom(any(), any<String>()) } answers {
            Result.failure(MatrixError.Forbidden(""))
        } andThenAnswer {
            testDeferred.complete(Unit)
            Result.success()
        }

        runBlocking {
            withTimeoutOrNull(10000) { p2pClient.subscribeTo(publicKey).collect() }

            coVerify(exactly = 2) { matrixClient.joinRoom(relayServer, roomId) }
        }
    }

    @Test
    fun `aborts join room on invite event if error other than denied access`() {
        val publicKey = "0x00".asHexString().toByteArray()
        val relayServer = "relayServer"
        val roomId = "1"

        val inviteMessages = listOf(
            validMatrixInviteMessage(relayServer, roomId, publicKey.toHexString().asString())
        )

        coEvery { p2pStore.state() } returns Result.success(
            P2pStoreState(
                relayServer = relayServer,
                availableNodes = 1,
            )
        )

        coEvery { matrixClient.isLoggedIn() } returns false andThen true
        every { matrixClient.events } returns flow {
            inviteMessages.forEach { emit(it) }
            testDeferred.await()
            delay(BeaconConfiguration.P2P_JOIN_DELAY_MS * 2)
        }
        coEvery { matrixClient.joinRoom(any(), any<String>()) } answers {
            testDeferred.complete(Unit)
            Result.failure(IOException())
        }

        runBlocking {
            withTimeoutOrNull(10000) { p2pClient.subscribeTo(publicKey).collect() }

            coVerify(exactly = 1) { matrixClient.joinRoom(relayServer, roomId) }
        }
    }

    private fun validMatrixTextMessage(
        node: String,
        roomId: String,
        senderHash: String,
        content: String,
    ): MatrixEvent.TextMessage =
        MatrixEvent.TextMessage(node, roomId, senderHash, content)

    private fun validMatrixInviteMessage(node: String, roomId: String, senderHash: String): MatrixEvent.Invite =
        MatrixEvent.Invite(node, roomId, senderHash)

}