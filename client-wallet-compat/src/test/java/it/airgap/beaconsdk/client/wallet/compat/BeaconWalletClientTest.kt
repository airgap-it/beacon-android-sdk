package it.airgap.beaconsdk.client.wallet.compat

import beaconConnectionMessageFlow
import beaconResponses
import beaconVersionedRequests
import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.client.wallet.BeaconWalletClient
import it.airgap.beaconsdk.core.data.MockAppMetadata
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.exception.BeaconException
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.controller.ConnectionController
import it.airgap.beaconsdk.core.internal.controller.MessageController
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.message.BeaconConnectionMessage
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.splitAt
import it.airgap.beaconsdk.core.internal.utils.success
import it.airgap.beaconsdk.core.message.AcknowledgeBeaconResponse
import it.airgap.beaconsdk.core.message.BeaconMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import mockDependencyRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import tryEmitValues
import versionedBeaconMessage
import java.io.IOException
import kotlin.test.assertEquals

internal class BeaconWalletClientTest {

    @MockK
    private lateinit var connectionController: ConnectionController

    @MockK
    private lateinit var messageController: MessageController

    @MockK
    private lateinit var identifierCreator: IdentifierCreator

    @MockK
    private lateinit var crypto: Crypto

    private lateinit var storageManager: StorageManager
    private lateinit var beaconWalletClient: BeaconWalletClient

    private lateinit var testDeferred: CompletableDeferred<Unit>

    private val appName: String = "mockApp"

    private val beaconId: String = "beaconId"

    private val dAppVersion: String = "2"
    private val dAppId: String = "dAppId"

    private val origin: Origin = Origin.P2P(dAppId)

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockkObject(BeaconCompat)

        coEvery { messageController.onIncomingMessage(any(), any()) } coAnswers  {
            Result.success(secondArg<VersionedBeaconMessage>().toBeaconMessage(origin))
        }

        coEvery { messageController.onOutgoingMessage(any(), any(), any()) } coAnswers {
            Result.success(Pair(secondArg<BeaconMessage>().associatedOrigin, versionedBeaconMessage(secondArg(), beaconId)))
        }

        coEvery { connectionController.send(any()) } coAnswers { Result.success() }

        val configuration = BeaconConfiguration(ignoreUnsupportedBlockchains = false)
        storageManager = StorageManager(MockStorage(), MockSecureStorage(), identifierCreator, configuration)
        beaconWalletClient = BeaconWalletClient(appName, beaconId, connectionController, messageController, storageManager, crypto, configuration)

        val dependencyRegistry = mockDependencyRegistry()
        every { dependencyRegistry.storageManager } returns storageManager
        every { dependencyRegistry.identifierCreator } returns identifierCreator
        every { dependencyRegistry.messageController } returns messageController

        testDeferred = CompletableDeferred()
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `connects for messages with callback`() {
        runBlockingTest {
            val requests = beaconVersionedRequests().shuffled()
            val beaconMessageFlow = beaconConnectionMessageFlow(requests.size + 1)

            every { connectionController.subscribe() } answers { beaconMessageFlow }

            storageManager.addAppMetadata(listOf(MockAppMetadata(dAppId, "otherApp")))

            runBlocking {
                val messages = mutableListOf<BeaconMessage>()
                val callback = spyk<OnNewMessageListener>(
                    object : OnNewMessageListener {
                        override fun onNewMessage(message: BeaconMessage) {
                            messages.add(message)

                            if (messages.size == requests.size) testDeferred.complete(Unit)
                        }

                        override fun onError(error: Throwable) {
                            throw IllegalStateException("Expected to get a new message", error)
                        }
                    }
                )
                beaconWalletClient.connect(callback)
                beaconMessageFlow.tryEmitValues(requests.map { BeaconConnectionMessage(origin, it) })

                testDeferred.await()

                val expected = requests.map { it.toBeaconMessage(origin) }

                assertEquals(expected.sortedBy { it.toString() }, messages.sortedBy { it.toString() })

                coVerify(exactly = expected.size) { messageController.onIncomingMessage(any(), any()) }
                coVerify(exactly = expected.size) { messageController.onOutgoingMessage(beaconId, match { it is AcknowledgeBeaconResponse }, false) }
                verify(exactly = requests.size) { callback.onNewMessage(any()) }
                verify(exactly = 0) { callback.onError(any()) }
                verify(exactly = 0) { callback.onCancel() }

                verify(exactly = 1) { BeaconCompat.addListener(callback) }
                assert(BeaconCompat.listeners.containsValue(callback))

                confirmVerified(messageController, callback)
            }
        }
    }

    @Test
    fun `responds to request`() {
        coEvery { connectionController.send(any()) } returns Result.success()

        val origin = Origin.P2P(dAppId)
        val responses = beaconResponses(version = dAppVersion, requestOrigin = origin).shuffled()

        val callback = spyk<ResponseCallback>(object : ResponseCallback {
            override fun onSuccess() {
                testDeferred.complete(Unit)
            }

            override fun onError(error: Throwable) {
                testDeferred.complete(Unit)
            }
        })

        responses.forEach {
            testDeferred = CompletableDeferred()

            val versioned = versionedBeaconMessage(it, beaconId)
            val expected = BeaconConnectionMessage(origin, versioned)

            beaconWalletClient.respond(it, callback)
            runBlocking { testDeferred.await() }

            coVerify { messageController.onOutgoingMessage(any(), it, any()) }
            coVerify { connectionController.send(expected) }
        }

        verify(exactly = responses.size) { callback.onSuccess() }
        verify(exactly = 0) { callback.onError(any()) }
        verify(exactly = 0) { callback.onCancel() }

        confirmVerified(messageController, connectionController, callback)
    }

    @Test
    fun `returns internal BeaconException when internal error occurred`() {
        runBlockingTest {
            val requests = beaconVersionedRequests().shuffled()
            val beaconMessageFlow = beaconConnectionMessageFlow(requests.size + 1)

            val exception = Exception()

            every { connectionController.subscribe() } answers { beaconMessageFlow }
            coEvery { messageController.onIncomingMessage(any(), any()) } returns Result.failure(exception)

            runBlocking {
                val errors = mutableListOf<Throwable>()
                val callback = spyk<OnNewMessageListener>(
                    object : OnNewMessageListener {
                        override fun onNewMessage(message: BeaconMessage) {
                            throw IllegalStateException("Expected to throw an error")
                        }

                        override fun onError(error: Throwable) {
                            errors.add(error)

                            if (errors.size == requests.size) testDeferred.complete(Unit)
                        }
                    }
                )

                beaconWalletClient.connect(callback)
                beaconMessageFlow.tryEmitValues(requests.map { BeaconConnectionMessage(origin, it) })

                testDeferred.await()

                val expected = errors.map { BeaconException.from(exception) }

                assertEquals(expected.map(Exception::toString).sorted(), errors.map(Throwable::toString).sorted())

                coVerify(exactly = requests.size) { messageController.onIncomingMessage(any(), any()) }
                verify(exactly = 0) { callback.onNewMessage(any()) }
                verify(exactly = requests.size) { callback.onError(any()) }
                verify(exactly = 0) { callback.onCancel() }

                confirmVerified(messageController, callback)
            }
        }
    }

    @Test
    fun `fails to respond when message sending failed`() {
        val error = IOException()
        coEvery { connectionController.send(any()) } returns Result.failure(error)

        val responses = beaconResponses().shuffled()

        val expected = responses.map { BeaconException.from(error) }
        val exceptions = mutableListOf<Throwable>()

        val callback = spyk<ResponseCallback>(object : ResponseCallback {
            override fun onSuccess() {
                testDeferred.complete(Unit)
            }

            override fun onError(error: Throwable) {
                exceptions.add(error)
                testDeferred.complete(Unit)
            }
        })

        responses.forEach {
            testDeferred = CompletableDeferred()

            beaconWalletClient.respond(it, callback)
            runBlocking { testDeferred.await() }
        }

        assertEquals(expected.map(Throwable::toString).sorted(), exceptions.map(Throwable::toString).sorted())

        coVerify(exactly = responses.size) { messageController.onOutgoingMessage(any(), any(), any()) }
        coVerify(exactly = responses.size) { connectionController.send(any()) }

        verify(exactly = 0) { callback.onSuccess() }
        verify(exactly = responses.size) { callback.onError(any()) }
        verify(exactly = 0) { callback.onCancel() }

        confirmVerified(messageController, connectionController, callback)
    }

    @Test
    fun `removes message callback`() {
        runBlockingTest {
            val requests = beaconVersionedRequests().shuffled()
            val (requestsForAll, requestsFor2) = requests.splitAt { it.size / 2 }
            val beaconMessageFlow = beaconConnectionMessageFlow(requests.size + 1)

            every { connectionController.subscribe() } answers { beaconMessageFlow }

            storageManager.addAppMetadata(listOf(MockAppMetadata(dAppId, "otherApp")))

            runBlocking {
                val testDeferred1 = CompletableDeferred<Unit>()
                val testDeferred2 = CompletableDeferred<Unit>()

                val messages1 = mutableListOf<BeaconMessage>()
                val messages2 = mutableListOf<BeaconMessage>()
                val callback1 = spyk<OnNewMessageListener>(
                    object : OnNewMessageListener {
                        override fun onNewMessage(message: BeaconMessage) {
                            messages1.add(message)

                            if (messages1.size == requestsForAll.size) testDeferred1.complete(Unit)
                        }

                        override fun onError(error: Throwable) {
                            throw IllegalStateException("Expected to get a new message", error)
                        }
                    }
                )
                val callback2 = spyk<OnNewMessageListener>(
                    object : OnNewMessageListener {
                        override fun onNewMessage(message: BeaconMessage) {
                            messages2.add(message)

                            if (messages2.size == requests.size) testDeferred2.complete(Unit)
                        }

                        override fun onError(error: Throwable) {
                            throw IllegalStateException("Expected to get a new message", error)
                        }
                    }
                )

                beaconWalletClient.connect(callback1)
                beaconWalletClient.connect(callback2)
                beaconMessageFlow.tryEmitValues(requestsForAll.map { BeaconConnectionMessage(origin, it) })

                testDeferred1.await()

                beaconWalletClient.disconnect(callback1)
                beaconMessageFlow.tryEmitValues(requestsFor2.map { BeaconConnectionMessage(origin, it) })

                testDeferred2.await()

                val expected1 = requestsForAll.map { it.toBeaconMessage(origin) }
                val expected2 = requests.map { it.toBeaconMessage(origin) }

                assertEquals(expected1.sortedBy { it.toString() }, messages1.sortedBy { it.toString() })
                assertEquals(expected2.sortedBy { it.toString() }, messages2.sortedBy { it.toString() })

                verify(exactly = requestsForAll.size) { callback1.onNewMessage(any()) }
                verify(exactly = requests.size) { callback2.onNewMessage(any()) }
                verify(exactly = 0) { callback1.onError(any()) }
                verify(exactly = 0) { callback2.onError(any()) }
                verify(exactly = 0) { callback1.onCancel() }
                verify(exactly = 0) { callback2.onCancel() }

                verify(exactly = 1) { BeaconCompat.removeListener(callback1) }
                verify(exactly = 0) { BeaconCompat.removeListener(match { it != callback1 }) }
                assert(!BeaconCompat.listeners.containsValue(callback1))
                assert(BeaconCompat.listeners.containsValue(callback2))
            }
        }
    }

    @Test
    fun `stops and removes all callbacks`() {
        runBlockingTest {
            val requests = beaconVersionedRequests().shuffled()
            val beaconMessageFlow = beaconConnectionMessageFlow(requests.size + 1)

            every { connectionController.subscribe() } answers { beaconMessageFlow }

            storageManager.addAppMetadata(listOf(MockAppMetadata(dAppId, "otherApp")))

            runBlocking {
                val testDeferred1 = CompletableDeferred<Unit>()
                val testDeferred2 = CompletableDeferred<Unit>()

                val messages1 = mutableListOf<BeaconMessage>()
                val messages2 = mutableListOf<BeaconMessage>()

                val callback1 = spyk<OnNewMessageListener>(
                    object : OnNewMessageListener {
                        override fun onNewMessage(message: BeaconMessage) {
                            messages1.add(message)

                            if (messages1.size == requests.size) testDeferred1.complete(Unit)
                        }

                        override fun onError(error: Throwable) {
                            throw IllegalStateException("Expected to get a new message", error)
                        }
                    }
                )
                val callback2 = spyk<OnNewMessageListener>(
                    object : OnNewMessageListener {
                        override fun onNewMessage(message: BeaconMessage) {
                            messages2.add(message)

                            if (messages2.size == requests.size) testDeferred2.complete(Unit)
                        }

                        override fun onError(error: Throwable) {
                            throw IllegalStateException("Expected to get a new message", error)
                        }
                    }
                )

                beaconWalletClient.connect(callback1)
                beaconWalletClient.connect(callback2)
                beaconMessageFlow.tryEmitValues(requests.map { BeaconConnectionMessage(origin, it) })

                testDeferred1.await()
                testDeferred2.await()

                beaconWalletClient.stop()

                val expected1 = requests.map { it.toBeaconMessage(origin) }
                val expected2 = requests.map { it.toBeaconMessage(origin) }

                assertEquals(expected1.sortedBy { it.toString() }, messages1.sortedBy { it.toString() })
                assertEquals(expected2.sortedBy { it.toString() }, messages2.sortedBy { it.toString() })

                verify(exactly = requests.size) { callback1.onNewMessage(any()) }
                verify(exactly = requests.size) { callback2.onNewMessage(any()) }
                verify(exactly = 0) { callback1.onError(any()) }
                verify(exactly = 0) { callback2.onError(any()) }
                verify(exactly = 1) { callback1.onCancel() }
                verify(exactly = 1) { callback2.onCancel() }


                verify(exactly = 1) { BeaconCompat.cancelScopes() }
                assert(BeaconCompat.listeners.isEmpty())
                assert(BeaconCompat.scopes.isEmpty())
            }
        }
    }
}