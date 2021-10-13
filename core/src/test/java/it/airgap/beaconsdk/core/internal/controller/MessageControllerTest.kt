package it.airgap.beaconsdk.core.internal.controller

import beaconMessages
import beaconResponses
import beaconVersionedMessages
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchainSerializer
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.toHexString
import kotlinx.coroutines.runBlocking
import mockBlockchainRegistry
import mockTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import permissionBeaconRequest
import permissionBeaconResponse
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class MessageControllerTest {

    @MockK
    private lateinit var blockchainRegistry: BlockchainRegistry

    @MockK
    private lateinit var identifierCreator: IdentifierCreator

    private val blockchain: MockBlockchain = MockBlockchain()

    private lateinit var storageManager: StorageManager
    private lateinit var messageController: MessageController

    private val currentTimeMillis: Long = 1

    private val version: String = "2"
    private val senderId: String = "00"

    private val origin = Origin.P2P(senderId)

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockBlockchainRegistry()
        mockTime(currentTimeMillis)

        every { blockchainRegistry.get(any()) } returns blockchain

        every { identifierCreator.accountIdentifier(any(), any()) } answers { Result.success(firstArg()) }
        every { identifierCreator.senderIdentifier(any()) } answers { Result.success(firstArg<ByteArray>().toHexString().asString()) }

        storageManager = StorageManager(MockStorage(), MockSecureStorage(), identifierCreator)
        messageController = MessageController(blockchainRegistry, storageManager, identifierCreator)
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `transforms incoming VersionedBeaconMessage to BeaconMessage`() {
        val versionedMessages = beaconVersionedMessages(version, senderId, includeError = false)

        versionedMessages.forEach {
            val beaconMessage = runBlocking { messageController.onIncomingMessage(origin, it).getOrNull() }
            val expected = runBlocking { it.toBeaconMessage(origin, storageManager) }

            assertEquals(expected, beaconMessage)
        }
    }

    @Test
    fun `transforms outgoing BeaconMessages to VersionedBeaconMessage`() {
        val origin = Origin.P2P(senderId)
        val beaconMessage = beaconMessages(version = version, origin = origin)

        beaconMessage.forEach {
            val pendingRequest = VersionedBeaconMessage.from(senderId, permissionBeaconRequest(id = it.id, senderId = senderId))
            runBlocking { messageController.onIncomingMessage(origin, pendingRequest) }

            runBlocking { println(storageManager.getAppMetadata()) }
            val versioned = runBlocking { messageController.onOutgoingMessage(senderId, it, true).getOrThrow() }
            val expected = runBlocking { Pair(origin, VersionedBeaconMessage.from(senderId, it)) }

            assertEquals(expected, versioned)
        }
    }

    @Test
    fun `saves app metadata on permission request`() {
        val permissionRequest = permissionBeaconRequest(version = version)
        val versionedRequest = VersionedBeaconMessage.from(senderId, permissionRequest)

        val result = runBlocking { messageController.onIncomingMessage(origin, versionedRequest) }
        val appsMetadata = runBlocking { storageManager.getAppMetadata() }

        assertTrue(result.isSuccess, "Expected result to be a success")
        assertEquals(listOf(permissionRequest.appMetadata), appsMetadata)
    }

    @Test
    fun `fails when processing response without matching pending request`() {
        val response = beaconResponses().shuffled().first()

        assertFailsWith<IllegalArgumentException> {
            runBlocking { messageController.onOutgoingMessage(senderId, response, true).getOrThrow() }
        }
    }

    @Test
    fun `saves permissions on permission response`() {
        val id = "id"

        val permissionRequest = permissionBeaconRequest(id = id, version = version, senderId = senderId)
        val permissionResponse = permissionBeaconResponse(id = id, version = version)

        val versionedRequest = VersionedBeaconMessage.from(senderId, permissionRequest)

        runBlocking {
            messageController.onIncomingMessage(origin, versionedRequest)
            messageController.onOutgoingMessage(senderId, permissionResponse, true)
        }

        val appMetadata = runBlocking { storageManager.getAppMetadata().first() }
        val permissions = runBlocking { storageManager.getPermissions() }
        val expected = listOf(
            MockBlockchainSerializer.MockPermission(
                MockBlockchain.IDENTIFIER,
                "@${permissionResponse.publicKey}",
                "@${permissionResponse.publicKey}",
                appMetadata.senderId,
                appMetadata,
                permissionResponse.publicKey,
                currentTimeMillis
            )
        )

        assertEquals(expected, permissions)
    }
}