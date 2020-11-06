package it.airgap.beaconsdk.internal.controller

import beaconMessages
import beaconResponses
import beaconVersionedMessages
import errorBeaconMessage
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.data.beacon.PermissionInfo
import it.airgap.beaconsdk.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.internal.protocol.Protocol
import it.airgap.beaconsdk.internal.protocol.ProtocolRegistry
import it.airgap.beaconsdk.internal.storage.MockStorage
import it.airgap.beaconsdk.internal.storage.decorator.DecoratedExtendedStorage
import it.airgap.beaconsdk.internal.utils.AccountUtils
import it.airgap.beaconsdk.internal.utils.Success
import kotlinx.coroutines.runBlocking
import mockTime
import org.junit.Before
import org.junit.Test
import permissionBeaconRequest
import permissionBeaconResponse
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class MessageControllerTest {

    @MockK
    private lateinit var protocol: Protocol

    @MockK
    private lateinit var protocolRegistry: ProtocolRegistry

    @MockK
    private lateinit var accountUtils: AccountUtils

    private lateinit var storage: DecoratedExtendedStorage
    private lateinit var messageController: MessageController

    private val currentTimeMillis: Long = 1

    private val version: String = "2"
    private val senderId: String = "senderId"

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockTime(currentTimeMillis)

        every { protocol.getAddressFromPublicKey(any()) } answers { Success(firstArg()) }
        every { protocolRegistry.get(any()) } returns protocol

        every { accountUtils.getAccountIdentifier(any(), any()) } answers { Success(firstArg()) }

        storage = DecoratedExtendedStorage(MockStorage())
        messageController = MessageController(protocolRegistry, storage, accountUtils)
    }

    @Test
    fun `transforms incoming VersionedBeaconMessage to BeaconMessage`() {
        val versionedMessages = beaconVersionedMessages(version, senderId, includeError = false)

        versionedMessages.forEach {
            val beaconMessage = runBlocking { messageController.onIncomingMessage(it).valueOrNull() }
            val expected = runBlocking { it.toBeaconMessage(storage) }

            assertEquals(expected, beaconMessage)
        }
    }

    @Test
    fun `transforms outgoing BeaconMessages to VersionedBeaconMessage`() {
        val beaconMessage = beaconMessages()

        beaconMessage.forEach {
            val pendingRequest = VersionedBeaconMessage.fromBeaconMessage(version, senderId, permissionBeaconRequest(id = it.id))
            runBlocking { messageController.onIncomingMessage(pendingRequest) }

            val versionedBeaconMessage = runBlocking { messageController.onOutgoingMessage(senderId, it).valueOrNull() }
            val expected = runBlocking { VersionedBeaconMessage.fromBeaconMessage(version, senderId, it) }

            assertEquals(expected, versionedBeaconMessage)
        }
    }

    @Test
    fun `saves app metadata on permission request`() {
        val permissionRequest = permissionBeaconRequest()
        val versionedRequest = VersionedBeaconMessage.fromBeaconMessage(version, senderId, permissionRequest)

        val result = runBlocking { messageController.onIncomingMessage(versionedRequest) }
        val appsMetadata = runBlocking { storage.getAppMetadata() }

        assertTrue(result.isSuccess, "Expected result to be a success")
        assertEquals(listOf(permissionRequest.appMetadata), appsMetadata)
    }

    @Test
    fun `returns failure on incoming error message`() {
        val errorMessage = errorBeaconMessage()
        val versionedMessage = VersionedBeaconMessage.fromBeaconMessage(version, senderId, errorMessage)

        val result = runBlocking { messageController.onIncomingMessage(versionedMessage) }

        assertTrue(result.isFailure, "Expected result to be a failure")
    }

    @Test
    fun `fails when processing response without matching pending request`() {
        val response = beaconResponses().shuffled().first()

        assertFailsWith<IllegalArgumentException> {
            runBlocking { messageController.onOutgoingMessage(senderId, response).value() }
        }
    }

    @Test
    fun `saves permissions on permission response`() {
        val id = "id"

        val permissionRequest = permissionBeaconRequest(id = id)
        val permissionResponse = permissionBeaconResponse(id = id)

        val versionedRequest = VersionedBeaconMessage.fromBeaconMessage(version, senderId, permissionRequest)

        runBlocking {
            messageController.onIncomingMessage(versionedRequest)
            messageController.onOutgoingMessage(senderId, permissionResponse)
        }

        val appMetadata = runBlocking { storage.getAppMetadata().first() }
        val permissions = runBlocking { storage.getPermissions() }
        val expected = listOf(
            PermissionInfo(
                permissionResponse.publicKey,
                permissionResponse.publicKey,
                permissionResponse.network,
                permissionResponse.scopes,
                appMetadata.senderId,
                appMetadata,
                permissionResponse.publicKey,
                currentTimeMillis
            )
        )

        assertEquals(expected, permissions)
    }

    @Test
    fun `fails to save permissions when app metadata not found`() {
        val id = "id"

        val permissionRequest = permissionBeaconRequest(id = id)
        val permissionResponse = permissionBeaconResponse(id = id)

        val versionedRequest = VersionedBeaconMessage.fromBeaconMessage(version, senderId, permissionRequest)

        runBlocking {
            messageController.onIncomingMessage(versionedRequest)
            storage.setAppMetadata(emptyList())
        }

        val result = runBlocking { messageController.onOutgoingMessage(senderId, permissionResponse) }

        assertTrue(result.isFailure, "Expected result to be a failure")
    }
}