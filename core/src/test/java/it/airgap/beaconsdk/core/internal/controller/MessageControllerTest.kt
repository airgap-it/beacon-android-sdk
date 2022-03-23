package it.airgap.beaconsdk.core.internal.controller

import beaconMessages
import beaconResponses
import beaconVersionedMessages
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import it.airgap.beaconsdk.core.data.MockPermission
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.toHexString
import kotlinx.coroutines.runBlocking
import mockDependencyRegistry
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
    private lateinit var identifierCreator: IdentifierCreator

    private lateinit var storageManager: StorageManager
    private lateinit var messageController: MessageController

    private val currentTimeMillis: Long = 1

    private val version: String = "2"
    private val senderId: String = "00"

    private val origin = Origin.P2P(senderId)

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockTime(currentTimeMillis)

        every { identifierCreator.accountId(any(), any()) } answers { Result.success(firstArg()) }
        every { identifierCreator.senderId(any()) } answers { Result.success(firstArg<ByteArray>().toHexString().asString()) }

        val dependencyRegistry = mockDependencyRegistry()
        storageManager = StorageManager(MockStorage(), MockSecureStorage(), identifierCreator, BeaconConfiguration(ignoreUnsupportedBlockchains = false))
        messageController = MessageController(dependencyRegistry.blockchainRegistry, storageManager, identifierCreator)

        every { dependencyRegistry.storageManager } returns storageManager
        every { dependencyRegistry.identifierCreator } returns identifierCreator
        every { dependencyRegistry.messageController } returns messageController
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
            val expected = runBlocking { it.toBeaconMessage(origin) }

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
            MockPermission(
                MockBlockchain.IDENTIFIER,
                "accountId",
                appMetadata.senderId,
                currentTimeMillis
            )
        )

        assertEquals(expected, permissions)
    }
}