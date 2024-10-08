package it.airgap.beaconsdk.client.wallet

import appMetadata
import beaconConnectionMessageFlow
import beaconResponses
import beaconVersionedRequests
import disconnectBeaconMessage
import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.data.*
import it.airgap.beaconsdk.core.exception.BeaconException
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.controller.connection.ConnectionController
import it.airgap.beaconsdk.core.internal.controller.message.MessageController
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.message.BeaconIncomingConnectionMessage
import it.airgap.beaconsdk.core.internal.message.BeaconOutgoingConnectionMessage
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.serializer.Serializer
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.*
import it.airgap.beaconsdk.core.message.AcknowledgeBeaconResponse
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.DisconnectBeaconMessage
import it.airgap.beaconsdk.core.scope.BeaconScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runBlockingTest
import mockDependencyRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import p2pPeers
import permissions
import tryEmitValues
import versionedBeaconMessage
import versionedBeaconMessageContext
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class BeaconWalletClientTest {

    @MockK
    private lateinit var connectionController: ConnectionController

    @MockK
    private lateinit var messageController: MessageController

    @MockK
    private lateinit var identifierCreator: IdentifierCreator

    @MockK
    private lateinit var crypto: Crypto

    @MockK
    private lateinit var serializer: Serializer

    private lateinit var dependencyRegistry: DependencyRegistry
    private lateinit var storageManager: StorageManager
    private lateinit var beaconWalletClient: BeaconWalletClient

    private val app: BeaconApplication = BeaconApplication(
        keyPair = KeyPair(byteArrayOf(0), byteArrayOf(0)),
        name = "mockApp",
    )

    private val beaconId: String = "beaconId"

    private val dAppVersion: String = "3"
    private val dAppId: String = "dAppId"
    private val dAppConnectionId: Connection.Id = Connection.Id.P2P(dAppId)

    private val walletId: String = "walletId"
    private val walletConnectionId: Connection.Id = Connection.Id.P2P(walletId)

    private val beaconScope: BeaconScope = BeaconScope.Global

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        coEvery { messageController.onIncomingMessage(any(), any(), any()) } coAnswers {
            Result.success(Pair(firstArg<Connection.Id>(), thirdArg<VersionedBeaconMessage>().toBeaconMessage(firstArg(), secondArg(), beaconScope)))
        }

        coEvery { messageController.onOutgoingMessage(any(), any(), any()) } coAnswers {
            Result.success(Pair(secondArg<BeaconMessage>().destination, versionedBeaconMessage(secondArg(), beaconId, dependencyRegistry.versionedBeaconMessageContext)))
        }

        coEvery { connectionController.send(any()) } coAnswers { Result.success() }

        every { crypto.guid() } returns Result.success("guid")

        val configuration = BeaconConfiguration(ignoreUnsupportedBlockchains = false)
        storageManager = StorageManager(beaconScope, MockStorage(), MockSecureStorage(), identifierCreator, configuration)
        beaconWalletClient = BeaconWalletClient(app, beaconId, beaconScope, connectionController, messageController, storageManager, crypto, serializer, configuration)


        dependencyRegistry = mockDependencyRegistry(beaconScope)
        every { dependencyRegistry.storageManager } returns storageManager
        every { dependencyRegistry.identifierCreator } returns identifierCreator
        every { dependencyRegistry.messageController } returns messageController
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `connects for messages flow`() {
        runBlockingTest {
            val requests = beaconVersionedRequests(dAppVersion, dAppId, dependencyRegistry.versionedBeaconMessageContext).shuffled()

            val beaconMessageFlow = beaconConnectionMessageFlow(requests.size + 1)

            every { connectionController.subscribe() } answers { beaconMessageFlow }

            storageManager.addAppMetadata(listOf(MockAppMetadata(dAppId, "otherApp")))

            val messages =
                beaconWalletClient.connect()
                    .onStart { beaconMessageFlow.tryEmitValues(requests.map { BeaconIncomingConnectionMessage(dAppConnectionId, it) }) }
                    .mapNotNull { it.getOrNull() }
                    .take(requests.size)
                    .toList()

            val expected = requests.map { it.toBeaconMessage(dAppConnectionId, Connection.Id.ownFrom(dAppConnectionId), beaconScope) }

            assertEquals(expected.sortedBy { it.toString() }, messages.sortedBy { it.toString() })
            coVerify(exactly = expected.size) { messageController.onIncomingMessage(any(), any(), any()) }
            coVerify(exactly = expected.size) { messageController.onOutgoingMessage(beaconId, match { it is AcknowledgeBeaconResponse }, false) }

            confirmVerified(messageController)
        }
    }

    @Test
    fun `responds to request`() {
        runBlockingTest {
            coEvery { connectionController.send(any()) } returns Result.success()

            val destination = Connection.Id.P2P(dAppId)
            val responses = beaconResponses(version = dAppVersion, destination = destination).shuffled()

            responses.forEach {
                val versioned = versionedBeaconMessage(it, beaconId, dependencyRegistry.versionedBeaconMessageContext)
                val expected = BeaconOutgoingConnectionMessage(destination, versioned)

                beaconWalletClient.respond(it)
                coVerify(exactly = 1) { connectionController.send(expected) }
            }

            confirmVerified(connectionController)
        }
    }

    @Test
    fun `emits BeaconException when internal error occurred`() {
        runBlockingTest {
            val requests = beaconVersionedRequests(context = dependencyRegistry.versionedBeaconMessageContext).shuffled()
            val beaconMessageFlow = beaconConnectionMessageFlow(requests.size + 1)

            val exception = Exception()

            every { connectionController.subscribe() } answers { beaconMessageFlow }
            coEvery { messageController.onIncomingMessage(any(), any(), any()) } returns Result.failure(exception)

            val errors =
                beaconWalletClient.connect()
                    .onStart { beaconMessageFlow.tryEmitValues(requests.map { BeaconIncomingConnectionMessage(dAppConnectionId, it) }) }
                    .mapNotNull { it.exceptionOrNull() }
                    .take(requests.size)
                    .toList()

            val expected = errors.map { BeaconException.from(exception) }

            assertEquals(expected.map(Exception::toString).sorted(), errors.map(Throwable::toString).sorted())
            coVerify(exactly = requests.size) { messageController.onIncomingMessage(any(), any(), any()) }

            confirmVerified(messageController)
        }
    }

    @Test
    fun `fails to respond when outgoing message processing failed with internal error`() {
        runBlockingTest {
            val error = IllegalStateException()
            coEvery { messageController.onOutgoingMessage(any(), any(), any()) } returns Result.failure(error)

            beaconResponses().forEach {
                val exception = assertFailsWith<BeaconException> { beaconWalletClient.respond(it) }

                assertEquals(error, exception.cause)
            }
        }
    }

    @Test
    fun `fails to respond when message sending failed`() {
        val error = IOException()
        runBlockingTest {
            coEvery { connectionController.send(any()) } returns Result.failure(error)

            val responses = beaconResponses().shuffled()

            responses.forEach {
                val exception = assertFailsWith<BeaconException> {
                    beaconWalletClient.respond(it)
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
            val origin = Connection.Id.P2P(publicKey)
            val peer = P2pPeer(name = "name", relayServer = "relayServer", publicKey = publicKey)
            storageManager.setPeers(listOf(peer))

            val versionedRequest = beaconVersionedRequests(dAppVersion, dAppId, dependencyRegistry.versionedBeaconMessageContext).shuffled().first()
            val connectionRequestMessage = BeaconIncomingConnectionMessage(origin, versionedRequest)

            val disconnectMessage = disconnectBeaconMessage(senderId = dAppId, destination = origin)
            val versionedDisconnectMessage = VersionedBeaconMessage.from(disconnectMessage.senderId, disconnectMessage, dependencyRegistry.versionedBeaconMessageContext)
            val connectionDisconnectMessage = BeaconIncomingConnectionMessage(disconnectMessage.destination, versionedDisconnectMessage)

            val beaconMessageFlow = beaconConnectionMessageFlow(2)
            every { connectionController.subscribe() } answers { beaconMessageFlow }

            storageManager.addAppMetadata(listOf(MockAppMetadata(dAppId, "otherApp")))

            beaconWalletClient.connect()
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

            with(beaconWalletClient) {
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

            val fromClient = beaconWalletClient.getPeers()

            assertEquals(storagePeers, fromClient)
        }
    }

    @Test
    fun `removes peers from storage and sends disconnect message`() {
        runBlockingTest {
            val (toKeep, toRemove) = p2pPeers(4).splitAt { it.size / 2 }

            val expectedDisconnectMessages = toRemove.map {
                val peerOrigin = Connection.Id.forPeer(it)
                DisconnectBeaconMessage(crypto.guid().getOrThrow(), beaconId, it.version, Connection.Id.ownFrom(peerOrigin), peerOrigin)
            }

            val expectedConnectionMessages = expectedDisconnectMessages.map {
                BeaconOutgoingConnectionMessage(it.destination to VersionedBeaconMessage.from(beaconId, it, dependencyRegistry.versionedBeaconMessageContext))
            }

            storageManager.setPeers(toKeep + toRemove)

            val (toRemoveVararg, toRemoveList) = toRemove.splitAt { it.size / 2 }
            with(beaconWalletClient) {
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
            beaconWalletClient.removePeers()

            val fromStorage = storageManager.getPeers()

            assertEquals(storagePeers, fromStorage)
            coVerify(exactly = 0) { connectionController.send(any()) }
        }
    }

    @Test
    fun `removes all peers from storage and sends disconnect messages`() {
        runBlockingTest {
            val peers = p2pPeers(4)

            val expectedDisconnectMessages = peers.map {
                val peerOrigin = Connection.Id.forPeer(it)
                DisconnectBeaconMessage(crypto.guid().getOrThrow(), beaconId, it.version, Connection.Id.ownFrom(peerOrigin), peerOrigin)
            }
            val expectedConnectionMessages = expectedDisconnectMessages.map {
                BeaconOutgoingConnectionMessage(it.destination to VersionedBeaconMessage.from(beaconId, it, dependencyRegistry.versionedBeaconMessageContext))
            }

            storageManager.setPeers(peers)
            beaconWalletClient.removeAllPeers()

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

            val fromClient = beaconWalletClient.getAppMetadata()

            assertEquals(storageMetadata, fromClient)
        }
    }

    @Test
    fun `returns app metadata matching specified sender ID`() {
        runBlockingTest {
            val storageMetadata = appMetadata(4)
            storageManager.setAppMetadata(storageMetadata)

            val toFind = storageMetadata.random()
            val fromClient = beaconWalletClient.getAppMetadataFor(toFind.senderId)

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
            with(beaconWalletClient) {
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
            with(beaconWalletClient) {
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
            beaconWalletClient.removeAppMetadata()

            val fromStorage = storageManager.getAppMetadata()

            assertEquals(storageAppMetadata, fromStorage)
        }
    }

    @Test
    fun `removes all app metadata from storage`() {
        runBlockingTest {
            val storageMetadata = appMetadata(4)
            storageManager.setAppMetadata(storageMetadata)
            beaconWalletClient.removeAllAppMetadata()

            val fromStorage = storageManager.getAppMetadata()

            assertTrue(fromStorage.isEmpty(), "Expected app metadata list to be empty")
        }
    }

    @Test
    fun `returns permissions from storage`() {
        runBlockingTest {
            val storagePermissions = permissions(4)
            storageManager.setPermissions(storagePermissions)

            val fromClient = beaconWalletClient.getPermissions()

            assertEquals(storagePermissions, fromClient)
        }
    }

    @Test
    fun `returns permissions matching specified account identifier`() {
        runBlockingTest {
            val storagePermissions = permissions(4)
            storageManager.setPermissions(storagePermissions)

            val toFind = storagePermissions.random()
            val fromClient = beaconWalletClient.getPermissionsFor(toFind.accountId)

            assertEquals(toFind, fromClient)
        }
    }


    @Test
    fun `removes permissions matching specified account IDs`() {
        runBlockingTest {
            val (toKeep, toRemove) = permissions(4).splitAt { it.size / 2 }
            storageManager.setPermissions(toKeep + toRemove)

            val (toRemoveVararg, toRemoveList) = toRemove.map(Permission::accountId)
                .splitAt { it.size / 2 }
            with(beaconWalletClient) {
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
            with(beaconWalletClient) {
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
            beaconWalletClient.removePermissions()

            val fromStorage = storageManager.getPermissions()

            assertEquals(storagePermissions, fromStorage)
        }
    }

    @Test
    fun `removes all permissions from storage`() {
        runBlockingTest {
            val storagePermissions = permissions(4)
            storageManager.setPermissions(storagePermissions)
            beaconWalletClient.removeAllPermissions()

            val fromStorage = storageManager.getPermissions()

            assertTrue(fromStorage.isEmpty(), "Expected app metadata list to be empty")
        }
    }

    private fun Connection.Id.Companion.ownFrom(destination: Connection.Id): Connection.Id =
        when (destination) {
            is Connection.Id.P2P -> destination.copy(id = app.keyPair.publicKey.toHexString().asString())
        }
}