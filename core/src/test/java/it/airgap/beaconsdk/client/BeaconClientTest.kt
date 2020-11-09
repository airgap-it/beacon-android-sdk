package it.airgap.beaconsdk.client

import appMetadata
import beaconOriginatedMessageFlow
import beaconResponses
import beaconVersionedRequests
import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.Origin
import it.airgap.beaconsdk.data.beacon.PermissionInfo
import it.airgap.beaconsdk.exception.BeaconException
import it.airgap.beaconsdk.internal.controller.ConnectionController
import it.airgap.beaconsdk.internal.controller.MessageController
import it.airgap.beaconsdk.internal.message.BeaconConnectionMessage
import it.airgap.beaconsdk.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.internal.storage.MockStorage
import it.airgap.beaconsdk.internal.storage.decorator.DecoratedExtendedStorage
import it.airgap.beaconsdk.internal.utils.Failure
import it.airgap.beaconsdk.internal.utils.Success
import it.airgap.beaconsdk.internal.utils.splitAt
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
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

    private lateinit var storage: DecoratedExtendedStorage
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
            Success(secondArg<VersionedBeaconMessage>().toBeaconMessage(origin, storage))
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

            val beaconMessageFlow = beaconOriginatedMessageFlow(requests.size + 1)

            every { connectionController.subscribe() } answers { beaconMessageFlow }

            storage.addAppMetadata(listOf(AppMetadata(dAppId, "otherApp")))

            val messages =
                beaconClient.connect()
                    .onStart { beaconMessageFlow.tryEmitValues(requests.map { BeaconConnectionMessage(origin, it) }) }
                    .mapNotNull { it.getOrNull() }
                    .take(requests.size)
                    .toList()

            val expected = requests.map { it.toBeaconMessage(origin, storage) }

            assertEquals(expected.sortedBy { it.toString() }, messages.sortedBy { it.toString() })
            coVerify(exactly = expected.size) { messageController.onIncomingMessage(any(), any()) }

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
    fun `emits BeaconException when internal error occurred`() {
        runBlockingTest {
            val requests = beaconVersionedRequests().shuffled()
            val beaconMessageFlow = beaconOriginatedMessageFlow(requests.size + 1)

            val exception = Exception()

            every { connectionController.subscribe() } answers { beaconMessageFlow }
            coEvery { messageController.onIncomingMessage(any(), any()) } returns Failure(exception)

            val errors =
                beaconClient.connect()
                    .onStart { beaconMessageFlow.tryEmitValues(requests.map { BeaconConnectionMessage(origin, it) }) }
                    .mapNotNull { it.exceptionOrNull() }
                    .take(requests.size)
                    .toList()

            val expected = errors.map { BeaconException(cause = exception) }

            assertEquals(expected.map(Exception::toString).sorted(), errors.map(Throwable::toString).sorted())
            coVerify(exactly = requests.size) { messageController.onIncomingMessage(any(), any()) }

            confirmVerified(messageController)
        }
    }

    @Test
    fun `fails to respond when outgoing message processing failed with internal error`() {
        runBlockingTest {
            val error = IllegalStateException()
            coEvery { messageController.onOutgoingMessage(any(), any()) } returns Failure(error)

            beaconResponses().forEach {
                assertFailsWith(error::class) {
                    beaconClient.respond(it)
                }
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
    fun `does not remove any P2P peer if not specified`() {
        runBlockingTest {
            val storagePeers = p2pPeers(4)
            storage.setP2pPeers(storagePeers)
            beaconClient.removePeers()

            val fromStorage = storage.getP2pPeers()

            assertEquals(storagePeers, fromStorage)
        }
    }

    @Test
    fun `removes all P2P peers from storage`() {
        runBlockingTest {
            storage.setP2pPeers(p2pPeers(4))
            beaconClient.removeAllPeers()

            val fromStorage = storage.getP2pPeers()

            assertTrue(fromStorage.isEmpty(), "Expected P2P peers list to be empty")
        }
    }

    @Test
    fun `returns app metadata from storage`() {
        runBlockingTest {
            val storageMetadata = appMetadata(4)
            storage.setAppMetadata(storageMetadata)

            val fromClient = beaconClient.getAppMetadata()

            assertEquals(storageMetadata, fromClient)
        }
    }

    @Test
    fun `returns app metadata matching specified sender ID`() {
        runBlockingTest {
            val storageMetadata = appMetadata(4)
            storage.setAppMetadata(storageMetadata)

            val toFind = storageMetadata.random()
            val fromClient = beaconClient.getAppMetadataFor(toFind.senderId)

            assertEquals(toFind, fromClient)
        }
    }

    @Test
    fun `removes app metadata matching specified sender IDs`() {
        runBlockingTest {
            val (toKeep, toRemove) = appMetadata(4).splitAt { it.size / 2 }
            storage.setAppMetadata(toKeep + toRemove)

            val (toRemoveVararg, toRemoveList) = toRemove.map(AppMetadata::senderId)
                .splitAt { it.size / 2 }
            with(beaconClient) {
                removeAppMetadataFor(*toRemoveVararg.toTypedArray())
                removeAppMetadataFor(toRemoveList)
            }

            val fromStorage = storage.getAppMetadata()

            assertEquals(toKeep, fromStorage)
        }
    }

    @Test
    fun `removes app metadata from storage`() {
        runBlockingTest {
            val (toKeep, toRemove) = appMetadata(4).splitAt { it.size / 2 }
            storage.setAppMetadata(toKeep + toRemove)

            val (toRemoveVararg, toRemoveList) = toRemove.splitAt { it.size / 2 }
            with(beaconClient) {
                removeAppMetadata(*toRemoveVararg.toTypedArray())
                removeAppMetadata(toRemoveList)
            }

            val fromStorage = storage.getAppMetadata()

            assertEquals(toKeep, fromStorage)
        }
    }

    @Test
    fun `does not remove any app metadata if not specified`() {
        runBlockingTest {
            val storageAppMetadata = appMetadata(4)
            storage.setAppMetadata(storageAppMetadata)
            beaconClient.removeAppMetadata()

            val fromStorage = storage.getAppMetadata()

            assertEquals(storageAppMetadata, fromStorage)
        }
    }

    @Test
    fun `removes all app metadata from storage`() {
        runBlockingTest {
            val storageMetadata = appMetadata(4)
            storage.setAppMetadata(storageMetadata)
            beaconClient.removeAllAppMetadata()

            val fromStorage = storage.getAppMetadata()

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
    fun `does not remove any permission if not specified`() {
        runBlockingTest {
            val storagePermissions = permissions(4)
            storage.setPermissions(storagePermissions)
            beaconClient.removePermissions()

            val fromStorage = storage.getPermissions()

            assertEquals(storagePermissions, fromStorage)
        }
    }

    @Test
    fun `removes all permissions from storage`() {
        runBlockingTest {
            val storagePermissions = permissions(4)
            storage.setPermissions(storagePermissions)
            beaconClient.removeAllPermissions()

            val fromStorage = storage.getPermissions()

            assertTrue(fromStorage.isEmpty(), "Expected app metadata list to be empty")
        }
    }
}