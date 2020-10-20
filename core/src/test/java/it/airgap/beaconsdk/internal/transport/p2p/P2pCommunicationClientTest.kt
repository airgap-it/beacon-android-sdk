package it.airgap.beaconsdk.internal.transport.p2p

import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.internal.BeaconConfig
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.internal.crypto.data.SessionKeyPair
import it.airgap.beaconsdk.internal.matrix.MatrixClient
import it.airgap.beaconsdk.internal.matrix.data.client.MatrixClientEvent
import it.airgap.beaconsdk.internal.matrix.data.client.MatrixClientMessage
import it.airgap.beaconsdk.internal.transport.p2p.data.P2pMessage
import it.airgap.beaconsdk.internal.transport.p2p.data.P2pHandshakeInfo
import it.airgap.beaconsdk.internal.utils.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class P2pCommunicationClientTest {

    @MockK
    private lateinit var matrixClient1: MatrixClient
    @MockK
    private lateinit var matrixClient2: MatrixClient

    @MockK
    private lateinit var crypto: Crypto

    private lateinit var p2pCommunicationClient: P2pCommunicationClient

    private val appName = "mockApp"
    private val replicationCount = 3
    private val keyPair = KeyPair(byteArrayOf(0), byteArrayOf(0))

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockkStatic("it.airgap.beaconsdk.internal.utils.LogKt")
        every { logDebug(any(), any()) } returns Unit

        every { crypto.hash(any<String>(), any()) } answers {
            val arg = firstArg<String>()
            val bytes = HexString.fromStringOrNull(arg)?.asByteArray() ?: arg.toByteArray()
            internalSuccess(bytes)
        }
        every { crypto.hash(any<HexString>(), any()) } answers { internalSuccess(firstArg<HexString>().asByteArray()) }
        every { crypto.hash(any<ByteArray>(), any()) } answers { internalSuccess(firstArg()) }

        every { crypto.hashKey(any<HexString>()) } answers { internalSuccess(firstArg<HexString>().asByteArray()) }
        every { crypto.hashKey(any<ByteArray>()) } answers { internalSuccess(firstArg()) }

        every { crypto.createServerSessionKeyPair(any(), any()) } answers { internalSuccess(SessionKeyPair(byteArrayOf(0), byteArrayOf(0))) }

        every { crypto.encryptMessageWithPublicKey(any<String>(), any()) } answers { internalSuccess(firstArg()) }
        every { crypto.encryptMessageWithPublicKey(any<HexString>(), any()) } answers { internalSuccess(firstArg<HexString>().value(withPrefix = false)) }

        every { crypto.encryptMessageWithSharedKey(any<String>(), any()) } answers { internalSuccess(firstArg()) }
        every { crypto.encryptMessageWithSharedKey(any<HexString>(), any()) } answers { internalSuccess(firstArg<HexString>().value(withPrefix = false)) }

        every { crypto.decryptMessageWithSharedKey(any<String>(), any()) } answers { internalSuccess(firstArg()) }
        every { crypto.decryptMessageWithSharedKey(any<HexString>(), any()) } answers { internalSuccess(firstArg<HexString>().value(withPrefix = false)) }

        p2pCommunicationClient = P2pCommunicationClient(
            appName,
            listOf(matrixClient1, matrixClient2),
            listOf(""),
            replicationCount,
            crypto,
            keyPair
        )
    }

    @Test
    fun `subscribes for messages`() {
        val publicKey = HexString.fromString("0x00")

        val messages1 = listOf(
            validMatrixTextMessage("1", publicKey.value(withPrefix = false), "content1")
        )
        val messages2 = listOf(
            validMatrixTextMessage("1", publicKey.value(withPrefix = false), "content2")
        )

        every { crypto.validateEncryptedMessage(any()) } returns true
        every { matrixClient1.events } returns flow<InternalResult<MatrixClientEvent<*>>> {
            messages1.forEach { emit(internalSuccess(it)) }
        }
        every { matrixClient2.events } returns flow<InternalResult<MatrixClientEvent<*>>> {
            messages2.forEach { emit(internalSuccess(it)) }
        }

        runBlocking {
            val expected = (messages1 + messages2).map { P2pMessage(publicKey.value(withPrefix = false), it.content.message.content) }
            val messages = p2pCommunicationClient.subscribeTo(publicKey).mapNotNull { it.getOrNull() }.toList()

           assertEquals(expected.sortedBy { it.content }, messages.sortedBy { it.content })
        }
    }

    @Test
    fun `ignores text messages from unsubscribed senders`() {
        val subscribedPublicKey = HexString.fromString("0x00")
        val unsubscribedPublicKey = HexString.fromString("0x01")

        val subscribedMessages = listOf(
            validMatrixTextMessage("1", subscribedPublicKey.value(withPrefix = false), "content1")
        )
        val unsubscribedMessages = listOf(
            validMatrixTextMessage("1", unsubscribedPublicKey.value(withPrefix = false), "content2")
        )

        every { crypto.validateEncryptedMessage(any()) } returns true
        every { matrixClient1.events } returns flow<InternalResult<MatrixClientEvent<*>>> {
            subscribedMessages.forEach { emit(internalSuccess(it)) }
        }
        every { matrixClient2.events } returns flow<InternalResult<MatrixClientEvent<*>>> {
            unsubscribedMessages.forEach { emit(internalSuccess(it)) }
        }

        runBlocking {
            val expected = subscribedMessages.map { P2pMessage(subscribedPublicKey.value(withPrefix = false), it.content.message.content) }
            val messages = p2pCommunicationClient.subscribeTo(subscribedPublicKey)
                .mapNotNull { it.getOrNull() }.toList()

            assertEquals(expected.sortedBy { it.content }, messages.sortedBy { it.content })
        }
    }

    @Test
    fun `ignores invalid text messages`() {
        val publicKey = HexString.fromString("0x00")

        val validMessages = listOf(
            validMatrixTextMessage("1", publicKey.value(withPrefix = false), "valid1")
        )
        val invalidMessages = listOf(
            validMatrixTextMessage("1", publicKey.value(withPrefix = false), "invalid2")
        )

        every { crypto.validateEncryptedMessage(match { it.startsWith("valid") }) } returns true
        every { crypto.validateEncryptedMessage(match { it.startsWith("invalid") }) } returns false

        every { matrixClient1.events } returns flow<InternalResult<MatrixClientEvent<*>>> {
            validMessages.forEach { emit(internalSuccess(it)) }
        }
        every { matrixClient2.events } returns flow<InternalResult<MatrixClientEvent<*>>> {
            invalidMessages.forEach { emit(internalSuccess(it)) }
        }

        runBlocking {
            val expected = validMessages.map { P2pMessage(publicKey.value(withPrefix = false), it.content.message.content) }
            val messages = p2pCommunicationClient.subscribeTo(publicKey).mapNotNull { it.getOrNull() }.toList()

            assertEquals(expected.sortedBy { it.content }, messages.sortedBy { it.content })
        }
    }

    @Test
    fun `ignores events other than text messages on subscribe`() {
        val publicKey = HexString.fromString("0x00")

        val textMessages1 = listOf(
            validMatrixTextMessage("1", publicKey.value(withPrefix = false), "content1")
        )
        val otherMessages1 = listOf(
            validMatrixInviteMessage("2")
        )

        val testMessages2 = listOf(
            validMatrixTextMessage("1", publicKey.value(withPrefix = false), "content2")
        )
        val otherMessages2 = listOf(
            validMatrixInviteMessage("2")
        )

        every { crypto.validateEncryptedMessage(any()) } returns true
        every { matrixClient1.events } returns flow<InternalResult<MatrixClientEvent<*>>> {
            textMessages1.forEach { emit(internalSuccess(it)) }
            otherMessages1.forEach { emit(internalSuccess(it)) }
        }
        every { matrixClient2.events } returns flow<InternalResult<MatrixClientEvent<*>>> {
            testMessages2.forEach { emit(internalSuccess(it)) }
            otherMessages2.forEach { emit(internalSuccess(it)) }
        }

        runBlocking {
            val expected = (textMessages1 + testMessages2).map { P2pMessage(publicKey.value(withPrefix = false), it.content.message.content) }
            val messages = p2pCommunicationClient.subscribeTo(publicKey).mapNotNull { it.getOrNull() }.toList()

            assertEquals(expected.sortedBy { it.content }, messages.sortedBy { it.content })
        }
    }

    @Test
    fun `ignores matrix errors on subscribe`() {
        val publicKey = HexString.fromString("0x00")

        val messages1 = listOf(
            validMatrixTextMessage("1", publicKey.value(withPrefix = false), "content1")
        )
        val messages2 = listOf(
            validMatrixTextMessage("1", publicKey.value(withPrefix = false), "content2")
        )
        val errors1 = listOf(internalError<MatrixClientEvent<*>>())
        val errors2 = listOf(internalError<MatrixClientEvent<*>>())

        every { crypto.validateEncryptedMessage(any()) } returns true
        every { matrixClient1.events } returns flow<InternalResult<MatrixClientEvent<*>>> {
            messages1.forEach { emit(internalSuccess(it)) }
            errors1.forEach { emit(it) }
        }
        every { matrixClient2.events } returns flow<InternalResult<MatrixClientEvent<*>>> {
            messages2.forEach { emit(internalSuccess(it)) }
            errors2.forEach { emit(it) }
        }

        runBlocking {
            val expected = (messages1 + messages2).map { P2pMessage(publicKey.value(withPrefix = false), it.content.message.content) }
            val messages = p2pCommunicationClient.subscribeTo(publicKey).mapNotNull { it.getOrNull() }.toList()

            assertEquals(expected.sortedBy { it.content }, messages.sortedBy { it.content })
        }
    }

    @Test
    fun `sends message to specified recipient`() {
        coEvery { matrixClient1.sendTextMessage(any<Any>(), any()) } returns Unit
        coEvery { matrixClient2.sendTextMessage(any<Any>(), any()) } returns Unit

        val publicKey = HexString.fromString("0x00")
        val message = "message"

        runBlocking { p2pCommunicationClient.sendTo(publicKey, message) }

        // TODO: verify rooms
        coVerify(exactly = replicationCount) { matrixClient1.sendTextMessage(any<Any>(), message) }
        coVerify(exactly = replicationCount) { matrixClient2.sendTextMessage(any<Any>(), message) }
    }

    @Test
    fun `sends pairing request to matrix clients`() {
        coEvery { matrixClient1.sendTextMessage(any<Any>(), any()) } returns Unit
        coEvery { matrixClient2.sendTextMessage(any<Any>(), any()) } returns Unit

        val publicKey = HexString.fromString("0x00")
        val relayServer = "relay_server"

        runBlocking { p2pCommunicationClient.sendPairingRequest(publicKey, relayServer) }

        val expectedRecipient = "@${publicKey.value(withPrefix = false)}:$relayServer"
        val expectedEncrypted = Json.encodeToString(P2pHandshakeInfo(
            appName,
            BeaconConfig.versionName,
            keyPair.publicKey.asHexString().value(withPrefix = false),
            ""
        ))
        val expectedMessage = "@channel-open:$expectedRecipient:$expectedEncrypted"

        // TODO: verify rooms
        coVerify(exactly = 1) { matrixClient1.sendTextMessage(any<Any>(), expectedMessage) }
        coVerify(exactly = 1) { matrixClient2.sendTextMessage(any<Any>(), expectedMessage) }
    }

    private fun validMatrixTextMessage(roomId: String, senderHash: String, content: String): MatrixClientEvent.Message.Text =
        MatrixClientEvent.Message.Text(MatrixClientEvent.Message.Content(roomId, MatrixClientMessage("@$senderHash", content)))

    private fun validMatrixInviteMessage(roomId: String): MatrixClientEvent.Invite =
        MatrixClientEvent.Invite(MatrixClientEvent.Invite.Content(roomId))

}