package it.airgap.beaconsdk.client

import appMetadata
import beaconConnectionMessageFlow
import beaconResponses
import beaconVersionedRequests
import disconnectBeaconMessage
import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.Origin
import it.airgap.beaconsdk.data.beacon.P2pPeer
import it.airgap.beaconsdk.data.beacon.Permission
import it.airgap.beaconsdk.exception.BeaconException
import it.airgap.beaconsdk.internal.controller.ConnectionController
import it.airgap.beaconsdk.internal.controller.MessageController
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.message.BeaconConnectionMessage
import it.airgap.beaconsdk.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.internal.storage.MockStorage
import it.airgap.beaconsdk.internal.storage.StorageManager
import it.airgap.beaconsdk.internal.utils.AccountUtils
import it.airgap.beaconsdk.internal.utils.Failure
import it.airgap.beaconsdk.internal.utils.Success
import it.airgap.beaconsdk.internal.utils.splitAt
import it.airgap.beaconsdk.message.AcknowledgeBeaconResponse
import it.airgap.beaconsdk.message.BeaconMessage
import it.airgap.beaconsdk.message.DisconnectBeaconMessage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import p2pPeers
import permissions
import tryEmitValues
import versionedBeaconMessage
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class BeaconClientTest {

    @MockK
    private lateinit var connectionController: ConnectionController

    @MockK
    private lateinit var messageController: MessageController

    @MockK
    private lateinit var accountUtils: AccountUtils

    @MockK
    private lateinit var crypto: Crypto

    private lateinit var storageManager: StorageManager
    private lateinit var beaconClient: BeaconClient

    private val appName: String = "mockApp"

    private val beaconId: String = "beaconId"

    private val dAppVersion: String = "2"
    private val dAppId: String = "dAppId"

    private val origin: Origin = Origin.P2P(dAppId)

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        coEvery { messageController.onIncomingMessage(any(), any()) } coAnswers {
            Success(secondArg<VersionedBeaconMessage>().toBeaconMessage(firstArg(), storageManager))
        }

        coEvery { messageController.onOutgoingMessage(any(), any(), any()) } coAnswers {
            Success(Pair(secondArg<BeaconMessage>().associatedOrigin, versionedBeaconMessage(secondArg(), beaconId)))
        }

        coEvery { connectionController.send(any()) } coAnswers { Success() }

        every { crypto.guid() } returns Success("guid")

        storageManager = StorageManager(MockStorage(), MockSecureStorage(), accountUtils)
        beaconClient = BeaconClient(appName, beaconId, connectionController, messageController, storageManager, crypto)
    }

    @Test
    fun `connects for messages flow`() {
        runBlockingTest {
            val requests = beaconVersionedRequests(dAppVersion, dAppId).shuffled()

            val beaconMessageFlow = beaconConnectionMessageFlow(requests.size + 1)

            every { connectionController.subscribe() } answers { beaconMessageFlow }

            storageManager.addAppMetadata(listOf(AppMetadata(dAppId, "otherApp")))

            val messages =
                beaconClient.connect()
                    .onStart { beaconMessageFlow.tryEmitValues(requests.map { BeaconConnectionMessage(origin, it) }) }
                    .mapNotNull { it.getOrNull() }
                    .take(requests.size)
                    .toList()

            val expected = requests.map { it.toBeaconMessage(origin, storageManager) }

            assertEquals(expected.sortedBy { it.toString() }, messages.sortedBy { it.toString() })
            coVerify(exactly = expected.size) { messageController.onIncomingMessage(any(), any()) }
            coVerify(exactly = expected.size) { messageController.onOutgoingMessage(beaconId, match { it is AcknowledgeBeaconResponse }, false) }

            confirmVerified(messageController)
        }
    }

    @Test
    fun `responds to request`() {
        runBlockingTest {
            coEvery { connectionController.send(any()) } returns Success()

            val origin = Origin.P2P(dAppId)
            val responses = beaconResponses(version = dAppVersion, requestOrigin = origin).shuffled()

            responses.forEach {
                val versioned = versionedBeaconMessage(it, beaconId)
                val expected = BeaconConnectionMessage(origin, versioned)

                beaconClient.respond(it)
                coVerify(exactly = 1) { connectionController.send(expected) }
            }

            confirmVerified(connectionController)
        }
    }

    @Test
    fun `emits BeaconException when internal error occurred`() {
        runBlockingTest {
            val requests = beaconVersionedRequests().shuffled()
            val beaconMessageFlow = beaconConnectionMessageFlow(requests.size + 1)

            val exception = Exception()

            every { connectionController.subscribe() } answers { beaconMessageFlow }
            coEvery { messageController.onIncomingMessage(any(), any()) } returns Failure(exception)

            val errors =
                beaconClient.connect()
                    .onStart { beaconMessageFlow.tryEmitValues(requests.map { BeaconConnectionMessage(origin, it) }) }
                    .mapNotNull { it.exceptionOrNull() }
                    .take(requests.size)
                    .toList()

            val expected = errors.map { BeaconException.from(exception) }

            assertEquals(expected.map(Exception::toString).sorted(), errors.map(Throwable::toString).sorted())
            coVerify(exactly = requests.size) { messageController.onIncomingMessage(any(), any()) }

            confirmVerified(messageController)
        }
    }

    @Test
    fun `fails to respond when outgoing message processing failed with internal error`() {
        runBlockingTest {
            val error = IllegalStateException()
            coEvery { messageController.onOutgoingMessage(any(), any(), any()) } returns Failure(error)

            beaconResponses().forEach {
                val exception = assertFailsWith<BeaconException> { beaconClient.respond(it) }

                assertEquals(error, exception.cause)
            }
        }
    }

    @Test
    fun `fails to respond when message sending failed`() {
        val error = IOException()
        runBlockingTest {
            coEvery { connectionController.send(any()) } returns Failure(error)

            val responses = beaconResponses().shuffled()

            responses.forEach {
                val exception = assertFailsWith<BeaconException> {
                    beaconClient.respond(it)
                }

                assertEquals(error, exception.cause)
            }

            coVerify(exactly = responses.size) { messageController.onOutgoingMessage(any(), any(), any()) }
            coVerify(exactly = responses.size) { connectionController.send(any()) }

            confirmVerified(messageController, connectionController)
        }
    }

    @Test
    fun `removes peer on disconnect message received`() {
        runBlockingTest {
            val publicKey = "publicKey"
            val origin = Origin.P2P(publicKey)
            val peer = P2pPeer(name = "name", relayServer = "relayServer", publicKey = publicKey)
            storageManager.setPeers(listOf(peer))

            val versionedRequest = beaconVersionedRequests(dAppVersion, dAppId).shuffled().first()
            val connectionRequestMessage = BeaconConnectionMessage(origin, versionedRequest)

            val disconnectMessage = disconnectBeaconMessage(senderId = dAppId, origin = origin)
            val versionedDisconnectMessage = VersionedBeaconMessage.fromBeaconMessage(disconnectMessage.senderId, disconnectMessage)
            val connectionDisconnectMessage = BeaconConnectionMessage(disconnectMessage.origin, versionedDisconnectMessage)

            val beaconMessageFlow = beaconConnectionMessageFlow(2)
            every { connectionController.subscribe() } answers { beaconMessageFlow }

            storageManager.addAppMetadata(listOf(AppMetadata(dAppId, "otherApp")))

            beaconClient.connect()
                .onStart { beaconMessageFlow.tryEmitValues(listOf(connectionDisconnectMessage, connectionRequestMessage)) }
                .mapNotNull { it.getOrNull() }
                .take(1)
                .single()

            val fromStorage = storageManager.getPeers()

            assertEquals(emptyList(), fromStorage)
        }
    }

    @Test
    fun `adds peers to storage`() {
        runBlockingTest {
            storageManager.setPeers(emptyList())

            val (newPeersVararg, newPeersList) = p2pPeers(4).splitAt { it.size / 2 }

            with(beaconClient) {
                addPeers(*newPeersVararg.toTypedArray())
                addPeers(newPeersList)
            }

            val expected = newPeersVararg + newPeersList
            val fromStorage = storageManager.getPeers()

            assertEquals(expected.sortedBy { it.name }, fromStorage.sortedBy { it.name })
        }
    }

    @Test
    fun `returns peers from storage`() {
        runBlockingTest {
            val storagePeers = p2pPeers(4)
            storageManager.setPeers(storagePeers)

            val fromClient = beaconClient.getPeers()

            assertEquals(storagePeers, fromClient)
        }
    }

    @Test
    fun `removes peers from storage and sends disconnect message`() {
        runBlockingTest {
            val (toKeep, toRemove) = p2pPeers(4).splitAt { it.size / 2 }

            val expectedDisconnectMessages =
                toRemove.map { DisconnectBeaconMessage(crypto.guid().get(), beaconId, it.version, Origin.forPeer(it)) }
            val expectedConnectionMessages =
                expectedDisconnectMessages.map { BeaconConnectionMessage(it.origin to VersionedBeaconMessage.fromBeaconMessage(beaconId, it)) }

            storageManager.setPeers(toKeep + toRemove)

            val (toRemoveVararg, toRemoveList) = toRemove.splitAt { it.size / 2 }
            with(beaconClient) {
                removePeers(*toRemoveVararg.toTypedArray())
                removePeers(toRemoveList)
            }

            val fromStorage = storageManager.getPeers()

            assertEquals(toKeep, fromStorage)
            coVerify(exactly = toRemove.count()) { connectionController.send(match { expectedConnectionMessages.contains(it) }) }
        }
    }

    @Test
    fun `does not remove any peer if not specified and does not send disconnect messages`() {
        runBlockingTest {
            val storagePeers = p2pPeers(4)
            storageManager.setPeers(storagePeers)
            beaconClient.removePeers()

            val fromStorage = storageManager.getPeers()

            assertEquals(storagePeers, fromStorage)
            coVerify(exactly = 0) { connectionController.send(any()) }
        }
    }

    @Test
    fun `removes all peers from storage and sends disconnect messages`() {
        runBlockingTest {
            val peers = p2pPeers(4)

            val expectedDisconnectMessages =
                peers.map { DisconnectBeaconMessage(crypto.guid().get(), beaconId, it.version, Origin.forPeer(it)) }
            val expectedConnectionMessages =
                expectedDisconnectMessages.map { BeaconConnectionMessage(it.origin to VersionedBeaconMessage.fromBeaconMessage(beaconId, it)) }

            storageManager.setPeers(peers)
            beaconClient.removeAllPeers()

            val fromStorage = storageManager.getPeers()

            assertTrue(fromStorage.isEmpty(), "Expected P2P peers list to be empty")
            coVerify(exactly = peers.count()) { connectionController.send(match { expectedConnectionMessages.contains(it) }) }
        }
    }

    @Test
    fun `returns app metadata from storage`() {
        runBlockingTest {
            val storageMetadata = appMetadata(4)
            storageManager.setAppMetadata(storageMetadata)

            val fromClient = beaconClient.getAppMetadata()

            assertEquals(storageMetadata, fromClient)
        }
    }

    @Test
    fun `returns app metadata matching specified sender ID`() {
        runBlockingTest {
            val storageMetadata = appMetadata(4)
            storageManager.setAppMetadata(storageMetadata)

            val toFind = storageMetadata.random()
            val fromClient = beaconClient.getAppMetadataFor(toFind.senderId)

            assertEquals(toFind, fromClient)
        }
    }

    @Test
    fun `removes app metadata matching specified sender IDs`() {
        runBlockingTest {
            val (toKeep, toRemove) = appMetadata(4).splitAt { it.size / 2 }
            storageManager.setAppMetadata(toKeep + toRemove)

            val (toRemoveVararg, toRemoveList) = toRemove.map(AppMetadata::senderId)
                .splitAt { it.size / 2 }
            with(beaconClient) {
                removeAppMetadataFor(*toRemoveVararg.toTypedArray())
                removeAppMetadataFor(toRemoveList)
            }

            val fromStorage = storageManager.getAppMetadata()

            assertEquals(toKeep, fromStorage)
        }
    }

    @Test
    fun `removes app metadata from storage`() {
        runBlockingTest {
            val (toKeep, toRemove) = appMetadata(4).splitAt { it.size / 2 }
            storageManager.setAppMetadata(toKeep + toRemove)

            val (toRemoveVararg, toRemoveList) = toRemove.splitAt { it.size / 2 }
            with(beaconClient) {
                removeAppMetadata(*toRemoveVararg.toTypedArray())
                removeAppMetadata(toRemoveList)
            }

            val fromStorage = storageManager.getAppMetadata()

            assertEquals(toKeep, fromStorage)
        }
    }

    @Test
    fun `does not remove any app metadata if not specified`() {
        runBlockingTest {
            val storageAppMetadata = appMetadata(4)
            storageManager.setAppMetadata(storageAppMetadata)
            beaconClient.removeAppMetadata()

            val fromStorage = storageManager.getAppMetadata()

            assertEquals(storageAppMetadata, fromStorage)
        }
    }

    @Test
    fun `removes all app metadata from storage`() {
        runBlockingTest {
            val storageMetadata = appMetadata(4)
            storageManager.setAppMetadata(storageMetadata)
            beaconClient.removeAllAppMetadata()

            val fromStorage = storageManager.getAppMetadata()

            assertTrue(fromStorage.isEmpty(), "Expected app metadata list to be empty")
        }
    }

    @Test
    fun `returns permissions from storage`() {
        runBlockingTest {
            val storagePermissions = permissions(4)
            storageManager.setPermissions(storagePermissions)

            val fromClient = beaconClient.getPermissions()

            assertEquals(storagePermissions, fromClient)
        }
    }

    @Test
    fun `returns permissions matching specified account identifier`() {
        runBlockingTest {
            val storagePermissions = permissions(4)
            storageManager.setPermissions(storagePermissions)

            val toFind = storagePermissions.random()
            val fromClient = beaconClient.getPermissionsFor(toFind.accountIdentifier)

            assertEquals(toFind, fromClient)
        }
    }


    @Test
    fun `removes permissions matching specified account IDs`() {
        runBlockingTest {
            val (toKeep, toRemove) = permissions(4).splitAt { it.size / 2 }
            storageManager.setPermissions(toKeep + toRemove)

            val (toRemoveVararg, toRemoveList) = toRemove.map(Permission::accountIdentifier)
                .splitAt { it.size / 2 }
            with(beaconClient) {
                removePermissionsFor(*toRemoveVararg.toTypedArray())
                removePermissionsFor(toRemoveList)
            }

            val fromStorage = storageManager.getPermissions()

            assertEquals(toKeep, fromStorage)
        }
    }

    @Test
    fun `removes permissions from storage`() {
        runBlockingTest {
            val (toKeep, toRemove) = permissions(4).splitAt { it.size / 2 }
            storageManager.setPermissions(toKeep + toRemove)

            val (toRemoveVararg, toRemoveList) = toRemove.splitAt { it.size / 2 }
            with(beaconClient) {
                removePermissions(*toRemoveVararg.toTypedArray())
                removePermissions(toRemoveList)
            }

            val fromStorage = storageManager.getPermissions()

            assertEquals(toKeep, fromStorage)
        }
    }

    @Test
    fun `does not remove any permission if not specified`() {
        runBlockingTest {
            val storagePermissions = permissions(4)
            storageManager.setPermissions(storagePermissions)
            beaconClient.removePermissions()

            val fromStorage = storageManager.getPermissions()

            assertEquals(storagePermissions, fromStorage)
        }
    }

    @Test
    fun `removes all permissions from storage`() {
        runBlockingTest {
            val storagePermissions = permissions(4)
            storageManager.setPermissions(storagePermissions)
            beaconClient.removeAllPermissions()

            val fromStorage = storageManager.getPermissions()

            assertTrue(fromStorage.isEmpty(), "Expected app metadata list to be empty")
        }
    }
}