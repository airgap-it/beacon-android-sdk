package it.airgap.beaconsdk.client

import appMetadata
import beaconErrors
import beaconResponses
import beaconVersionedRequests
import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.PermissionInfo
import it.airgap.beaconsdk.exception.BeaconException
import it.airgap.beaconsdk.internal.controller.ConnectionController
import it.airgap.beaconsdk.internal.controller.MessageController
import it.airgap.beaconsdk.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.internal.storage.MockStorage
import it.airgap.beaconsdk.internal.storage.decorator.DecoratedExtendedStorage
import it.airgap.beaconsdk.internal.utils.Failure
import it.airgap.beaconsdk.internal.utils.Success
import it.airgap.beaconsdk.internal.utils.splitAt
import it.airgap.beaconsdk.message.ErrorBeaconMessage
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import p2pPeers
import permissions
import tryEmit
import versionedBeaconMessage
import versionedBeaconMessageFlow
import versionedBeaconMessages
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class BeaconClientTest {

    @MockK
    private lateinit var connectionController: ConnectionController

    @MockK
    private lateinit var messageController: MessageController

    private lateinit var storage: DecoratedExtendedStorage
    private lateinit var beaconClient: BeaconClient

    private val appName: String = "mockApp"

    private val beaconId: String = "beaconId"

    private val dAppVersion: String = "2"
    private val dAppId: String = "dAppId"

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        coEvery { messageController.onIncomingMessage(any()) } coAnswers {
            when (val beaconMessage = firstArg<VersionedBeaconMessage>().toBeaconMessage(storage)) {
                is ErrorBeaconMessage -> Failure(BeaconException.fromType(beaconMessage.errorType))
                else -> Success(beaconMessage)
            }
        }

        coEvery { messageController.onOutgoingMessage(any(), any()) } coAnswers {
            Success(versionedBeaconMessage(secondArg(), dAppVersion, beaconId))
        }

        storage = DecoratedExtendedStorage(MockStorage())
        beaconClient = BeaconClient(appName, beaconId, connectionController, messageController, storage)
    }

    @Test
    fun `connects for messages flow`() {
        runBlockingTest {
            val requests = beaconVersionedRequests(dAppVersion, dAppId).shuffled()
            val beaconMessageFlow = versionedBeaconMessageFlow(requests.size + 1)

            every { connectionController.subscribe() } answers { beaconMessageFlow }

            storage.addAppsMetadata(AppMetadata(dAppId, "otherApp"))

            val messages =
                beaconClient.connect()
                    .onStart { beaconMessageFlow.tryEmit(requests) }
                    .mapNotNull { it.getOrNull() }
                    .take(requests.size)
                    .toList()

            val expected = requests.map { it.toBeaconMessage(storage) }

            assertEquals(expected.sortedBy { it.toString() }, messages.sortedBy { it.toString() })
            coVerify(exactly = expected.size) { messageController.onIncomingMessage(any()) }

            confirmVerified(messageController)
        }
    }

    @Test
    fun `responds to request`() {
        runBlockingTest {
            coEvery { connectionController.send(any()) } returns Success()

            val responses = beaconResponses().shuffled()

            responses.forEach {
                val expected = versionedBeaconMessage(it, dAppVersion, beaconId)

                beaconClient.respond(it)
                coVerify(exactly = 1) { connectionController.send(expected) }
            }

            confirmVerified(connectionController)
        }
    }

    @Test
    fun `emits internal BeaconException when internal error occurred`() {
        runBlockingTest {
            val requests = beaconVersionedRequests().shuffled()
            val beaconMessageFlow = versionedBeaconMessageFlow(requests.size + 1)

            val exception = Exception()

            every { connectionController.subscribe() } answers { beaconMessageFlow }
            coEvery { messageController.onIncomingMessage(any()) } returns Failure(exception)

            val errors =
                beaconClient.connect()
                    .onStart { beaconMessageFlow.tryEmit(requests) }
                    .mapNotNull { it.exceptionOrNull() }
                    .take(requests.size)
                    .toList()

            val expected = errors.map { BeaconException.Internal(cause = exception) }

            assertEquals(expected.map(Exception::toString).sorted(), errors.map(Throwable::toString).sorted())
            coVerify(exactly = requests.size) { messageController.onIncomingMessage(any()) }

            confirmVerified(messageController)
        }
    }

    @Test
    fun `emits specific BeaconException when known error occurred`() {
        runBlockingTest {
            val errors = beaconErrors().shuffled()
            val beaconMessageFlow = versionedBeaconMessageFlow(errors.size + 1)

            every { connectionController.subscribe() } answers { beaconMessageFlow }

            val exceptions =
                beaconClient.connect()
                    .onStart { beaconMessageFlow.tryEmit(versionedBeaconMessages(errors)) }
                    .mapNotNull { it.exceptionOrNull() }
                    .take(errors.size)
                    .toList()

            val expected = errors.map { BeaconException.fromType(it.errorType) }

            assertEquals(expected.map(Exception::toString).sorted(), exceptions.map(Throwable::toString).sorted())
            coVerify(exactly = errors.size) { messageController.onIncomingMessage(any()) }

            confirmVerified(messageController)
        }
    }

    @Test
    fun `fails to respond when outgoing message processing failed with internal error`() {
        runBlockingTest {
            coEvery { messageController.onOutgoingMessage(any(), any()) } returns Failure(IllegalStateException())

            beaconResponses().forEach {
                assertFailsWith<BeaconException.Internal> {
                    beaconClient.respond(it)
                }
            }
        }
    }

    @Test
    fun `fails to respond when outgoing message processing failed with known Beacon error`() {
        runBlockingTest {
            val errors = beaconErrors().shuffled()
            val responses = beaconResponses().shuffled()

            errors.forEach { error ->
                val beaconException = BeaconException.fromType(error.errorType)
                coEvery { messageController.onOutgoingMessage(any(), any()) } returns Failure(beaconException)

                responses.forEach { response ->
                    val exception = assertFailsWith<BeaconException> { beaconClient.respond(response) }
                    assertEquals(beaconException, exception)
                }
            }

            coVerify(exactly = responses.size * errors.size) { messageController.onOutgoingMessage(any(), any()) }
            coVerify(exactly = 0) { connectionController.send(any()) }

            confirmVerified(messageController, connectionController)
        }
    }

    @Test
    fun `fails to respond when message sending failed`() {
        runBlockingTest {
            coEvery { connectionController.send(any()) } returns Failure()

            val responses = beaconResponses().shuffled()

            responses.forEach {
                assertFailsWith<BeaconException.Internal> {
                    beaconClient.respond(it)
                }
            }

            coVerify(exactly = responses.size) { messageController.onOutgoingMessage(any(), any()) }
            coVerify(exactly = responses.size) { connectionController.send(any()) }

            confirmVerified(messageController, connectionController)
        }
    }

    @Test
    fun `adds P2P peers to storage`() {
        runBlockingTest {
            storage.setP2pPeers(emptyList())

            val (newPeersVararg, newPeersList) = p2pPeers(4).splitAt { it.size / 2 }

            with(beaconClient) {
                addPeers(*newPeersVararg.toTypedArray())
                addPeers(newPeersList)
            }

            val expected = newPeersVararg + newPeersList
            val fromStorage = storage.getP2pPeers()

            assertEquals(expected.sortedBy { it.name }, fromStorage.sortedBy { it.name })
        }
    }

    @Test
    fun `returns P2P peers from storage`() {
        runBlockingTest {
            val storagePeers = p2pPeers(4)
            storage.setP2pPeers(storagePeers)

            val fromClient = beaconClient.getPeers()

            assertEquals(storagePeers, fromClient)
        }
    }

    @Test
    fun `removes P2P peers from storage`() {
        runBlockingTest {
            val (toKeep, toRemove) = p2pPeers(4).splitAt { it.size / 2 }
            storage.setP2pPeers(toKeep + toRemove)

            val (toRemoveVararg, toRemoveList) = toRemove.splitAt { it.size / 2 }
            with(beaconClient) {
                removePeers(*toRemoveVararg.toTypedArray())
                removePeers(toRemoveList)
            }

            val fromStorage = storage.getP2pPeers()

            assertEquals(toKeep, fromStorage)
        }
    }

    @Test
    fun `removes all P2P peers from storage`() {
        runBlockingTest {
            storage.setP2pPeers(p2pPeers(4))
            beaconClient.removePeers()

            val fromStorage = storage.getP2pPeers()

            assertTrue(fromStorage.isEmpty(), "Expected P2P peers list to be empty")
        }
    }

    @Test
    fun `returns apps metadata from storage`() {
        runBlockingTest {
            val storageMetadata = appMetadata(4)
            storage.setAppsMetadata(storageMetadata)

            val fromClient = beaconClient.getAppsMetadata()

            assertEquals(storageMetadata, fromClient)
        }
    }

    @Test
    fun `returns app metadata matching specified sender ID`() {
        runBlockingTest {
            val storageMetadata = appMetadata(4)
            storage.setAppsMetadata(storageMetadata)

            val toFind = storageMetadata.random()
            val fromClient = beaconClient.getAppMetadataFor(toFind.senderId)

            assertEquals(toFind, fromClient)
        }
    }

    @Test
    fun `removes app metadata matching specified sender IDs`() {
        runBlockingTest {
            val (toKeep, toRemove) = appMetadata(4).splitAt { it.size / 2 }
            storage.setAppsMetadata(toKeep + toRemove)

            val (toRemoveVararg, toRemoveList) = toRemove.map(AppMetadata::senderId)
                .splitAt { it.size / 2 }
            with(beaconClient) {
                removeAppsMetadataFor(*toRemoveVararg.toTypedArray())
                removeAppsMetadataFor(toRemoveList)
            }

            val fromStorage = storage.getAppsMetadata()

            assertEquals(toKeep, fromStorage)
        }
    }

    @Test
    fun `removes app metadata from storage`() {
        runBlockingTest {
            val (toKeep, toRemove) = appMetadata(4).splitAt { it.size / 2 }
            storage.setAppsMetadata(toKeep + toRemove)

            val (toRemoveVararg, toRemoveList) = toRemove.splitAt { it.size / 2 }
            with(beaconClient) {
                removeAppsMetadata(*toRemoveVararg.toTypedArray())
                removeAppsMetadata(toRemoveList)
            }

            val fromStorage = storage.getAppsMetadata()

            assertEquals(toKeep, fromStorage)
        }
    }

    @Test
    fun `removes all app metadata from storage`() {
        runBlockingTest {
            val storageMetadata = appMetadata(4)
            storage.setAppsMetadata(storageMetadata)
            beaconClient.removeAppsMetadata()

            val fromStorage = storage.getAppsMetadata()

            assertTrue(fromStorage.isEmpty(), "Expected app metadata list to be empty")
        }
    }

    @Test
    fun `returns permissions from storage`() {
        runBlockingTest {
            val storagePermissions = permissions(4)
            storage.setPermissions(storagePermissions)

            val fromClient = beaconClient.getPermissions()

            assertEquals(storagePermissions, fromClient)
        }
    }

    @Test
    fun `returns permissions matching specified account identifier`() {
        runBlockingTest {
            val storagePermissions = permissions(4)
            storage.setPermissions(storagePermissions)

            val toFind = storagePermissions.random()
            val fromClient = beaconClient.getPermissionsFor(toFind.accountIdentifier)

            assertEquals(toFind, fromClient)
        }
    }


    @Test
    fun `removes permissions matching specified account IDs`() {
        runBlockingTest {
            val (toKeep, toRemove) = permissions(4).splitAt { it.size / 2 }
            storage.setPermissions(toKeep + toRemove)

            val (toRemoveVararg, toRemoveList) = toRemove.map(PermissionInfo::accountIdentifier)
                .splitAt { it.size / 2 }
            with(beaconClient) {
                removePermissionsFor(*toRemoveVararg.toTypedArray())
                removePermissionsFor(toRemoveList)
            }

            val fromStorage = storage.getPermissions()

            assertEquals(toKeep, fromStorage)
        }
    }

    @Test
    fun `removes permissions from storage`() {
        runBlockingTest {
            val (toKeep, toRemove) = permissions(4).splitAt { it.size / 2 }
            storage.setPermissions(toKeep + toRemove)

            val (toRemoveVararg, toRemoveList) = toRemove.splitAt { it.size / 2 }
            with(beaconClient) {
                removePermissions(*toRemoveVararg.toTypedArray())
                removePermissions(toRemoveList)
            }

            val fromStorage = storage.getPermissions()

            assertEquals(toKeep, fromStorage)
        }
    }

    @Test
    fun `removes all permissions from storage`() {
        runBlockingTest {
            val storagePermissions = permissions(4)
            storage.setPermissions(storagePermissions)
            beaconClient.removePermissions()

            val fromStorage = storage.getPermissions()

            assertTrue(fromStorage.isEmpty(), "Expected app metadata list to be empty")
        }
    }
}