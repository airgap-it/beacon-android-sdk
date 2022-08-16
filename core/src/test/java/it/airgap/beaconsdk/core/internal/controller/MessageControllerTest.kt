package it.airgap.beaconsdk.core.internal.controller

import beaconMessages
import beaconResponses
import beaconVersionedMessages
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.data.MockPermission
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.compat.CoreCompat
import it.airgap.beaconsdk.core.internal.controller.message.MessageController
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.toHexString
import it.airgap.beaconsdk.core.scope.BeaconScope
import kotlinx.coroutines.runBlocking
import mockDependencyRegistry
import mockTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import permissionBeaconRequest
import permissionBeaconResponse
import versionedBeaconMessageContext
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class MessageControllerTest {

    @MockK
    private lateinit var identifierCreator: IdentifierCreator

    private lateinit var dependencyRegistry: DependencyRegistry
    private lateinit var storageManager: StorageManager
    private lateinit var messageController: MessageController

    private val currentTimeMillis: Long = 1

    private val version: String = "2"

    private val senderId: String = "00"
    private val receiverId: String = "01"

    private val origin = Connection.Id.P2P(senderId)
    private val destination = Connection.Id.P2P(receiverId)

    private val beaconScope: BeaconScope = BeaconScope.Global

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockTime(currentTimeMillis)

        every { identifierCreator.accountId(any(), any()) } answers { Result.success(firstArg()) }
        every { identifierCreator.senderId(any()) } answers { Result.success(firstArg<ByteArray>().toHexString().asString()) }

        dependencyRegistry = mockDependencyRegistry()
        every { dependencyRegistry.compat } returns CoreCompat(beaconScope)

        storageManager = StorageManager(beaconScope, MockStorage(), MockSecureStorage(), identifierCreator, BeaconConfiguration(ignoreUnsupportedBlockchains = false))
        messageController = MessageController(beaconScope, dependencyRegistry.blockchainRegistry, storageManager, identifierCreator, dependencyRegistry.compat)

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
        val versionedMessages = beaconVersionedMessages(version, senderId, dependencyRegistry.versionedBeaconMessageContext, includeError = false)

        versionedMessages.forEach {
            val pendingRequest = permissionBeaconRequest(id = runBlocking { it.toBeaconMessage(origin, destination, beaconScope).id }, senderId = senderId)
            runBlocking { messageController.onOutgoingMessage(senderId, pendingRequest, false) }

            val (incomingOrigin, beaconMessage) = runBlocking { messageController.onIncomingMessage(origin, destination, it).getOrThrow() }
            val expected = runBlocking { it.toBeaconMessage(origin, destination, beaconScope) }

            assertEquals(origin, incomingOrigin)
            assertEquals(expected, beaconMessage)
        }
    }

    @Test
    fun `transforms outgoing BeaconMessages to VersionedBeaconMessage`() {
        val origin = Connection.Id.P2P(senderId)
        val destination = Connection.Id.P2P(receiverId)
        val beaconMessage = beaconMessages(version = version, origin = origin, destination = destination)

        beaconMessage.forEach {
            val pendingRequest = VersionedBeaconMessage.from(senderId, permissionBeaconRequest(id = it.id, senderId = senderId), dependencyRegistry.versionedBeaconMessageContext)
            runBlocking { messageController.onIncomingMessage(origin, destination, pendingRequest) }

            val versioned = runBlocking { messageController.onOutgoingMessage(senderId, it, true).getOrThrow() }
            val expected = runBlocking { Pair(destination, VersionedBeaconMessage.from(senderId, it, dependencyRegistry.versionedBeaconMessageContext)) }

            assertEquals(expected, versioned)
        }
    }

    @Test
    fun `saves app metadata on permission request`() {
        val permissionRequest = permissionBeaconRequest(version = version)
        val versionedRequest = VersionedBeaconMessage.from(senderId, permissionRequest, dependencyRegistry.versionedBeaconMessageContext)

        val result = runBlocking { messageController.onIncomingMessage(origin, destination, versionedRequest) }
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

        val versionedRequest = VersionedBeaconMessage.from(senderId, permissionRequest, dependencyRegistry.versionedBeaconMessageContext)

        runBlocking {
            messageController.onIncomingMessage(origin, destination, versionedRequest)
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