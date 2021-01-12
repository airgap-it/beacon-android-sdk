package it.airgap.beaconsdk.compat.client

import beaconConnectionMessageFlow
import beaconResponses
import beaconVersionedRequests
import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.client.BeaconClient
import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.Origin
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
import it.airgap.beaconsdk.message.AcknowledgeBeaconResponse
import it.airgap.beaconsdk.message.BeaconMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import tryEmitValues
import versionedBeaconMessage
import java.io.IOException
import kotlin.test.assertEquals

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

    private lateinit var testDeferred: CompletableDeferred<Unit>

    private val appName: String = "mockApp"

    private val beaconId: String = "beaconId"

    private val dAppVersion: String = "2"
    private val dAppId: String = "dAppId"

    private val origin: Origin = Origin.P2P(dAppId)

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        coEvery { messageController.onIncomingMessage(any(), any()) } coAnswers  {
            Success(secondArg<VersionedBeaconMessage>().toBeaconMessage(origin, storageManager))
        }

        coEvery { messageController.onOutgoingMessage(any(), any(), any()) } coAnswers {
            Success(Pair(secondArg<BeaconMessage>().associatedOrigin, versionedBeaconMessage(secondArg(), beaconId)))
        }

        coEvery { connectionController.send(any()) } coAnswers { Success() }

        storageManager = StorageManager(MockStorage(), MockSecureStorage(), accountUtils)
        beaconClient = BeaconClient(appName, beaconId, connectionController, messageController, storageManager, crypto)

        testDeferred = CompletableDeferred()
    }

    @Test
    fun `connects for messages with callback`() {
        runBlockingTest {
            val requests = beaconVersionedRequests().shuffled()
            val beaconMessageFlow = beaconConnectionMessageFlow(requests.size + 1)

            every { connectionController.subscribe() } answers { beaconMessageFlow }

            storageManager.addAppMetadata(listOf(AppMetadata(dAppId, "otherApp")))

            runBlocking {
                val messages = mutableListOf<BeaconMessage>()
                val callback = spyk<OnNewMessageListener>(
                    object : OnNewMessageListener {
                        override fun onNewMessage(message: BeaconMessage) {
                            messages.add(message)

                            if (messages.size == requests.size) testDeferred.complete(Unit)
                        }

                        override fun onError(error: Throwable) = Unit
                    }
                )
                beaconClient.connect(callback)
                beaconMessageFlow.tryEmitValues(requests.map { BeaconConnectionMessage(origin, it) })

                testDeferred.await()

                val expected = requests.map { it.toBeaconMessage(origin, storageManager) }

                assertEquals(expected.sortedBy { it.toString() }, messages.sortedBy { it.toString() })

                coVerify(exactly = expected.size) { messageController.onIncomingMessage(any(), any()) }
                coVerify(exactly = expected.size) { messageController.onOutgoingMessage(beaconId, match { it is AcknowledgeBeaconResponse }, false) }
                verify(exactly = requests.size) { callback.onNewMessage(any()) }
                verify(exactly = 0) { callback.onError(any()) }

                confirmVerified(messageController, callback)
            }
        }
    }

    @Test
    fun `responds to request`() {
        coEvery { connectionController.send(any()) } returns Success()

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

            beaconClient.respond(it, callback)
            runBlocking { testDeferred.await() }

            coVerify { messageController.onOutgoingMessage(any(), it, any()) }
            coVerify { connectionController.send(expected) }
        }

        verify(exactly = responses.size) { callback.onSuccess() }
        verify(exactly = 0) { callback.onError(any()) }

        confirmVerified(messageController, connectionController, callback)
    }

    @Test
    fun `returns internal BeaconException when internal error occurred`() {
        runBlockingTest {
            val requests = beaconVersionedRequests().shuffled()
            val beaconMessageFlow = beaconConnectionMessageFlow(requests.size + 1)

            val exception = Exception()

            every { connectionController.subscribe() } answers { beaconMessageFlow }
            coEvery { messageController.onIncomingMessage(any(), any()) } returns Failure(exception)

            runBlocking {
                val errors = mutableListOf<Throwable>()
                val callback = spyk<OnNewMessageListener>(
                    object : OnNewMessageListener {
                        override fun onNewMessage(message: BeaconMessage) = Unit

                        override fun onError(error: Throwable) {
                            errors.add(error)

                            if (errors.size == requests.size) testDeferred.complete(Unit)
                        }
                    }
                )

                beaconClient.connect(callback)
                beaconMessageFlow.tryEmitValues(requests.map { BeaconConnectionMessage(origin, it) })

                testDeferred.await()

                val expected = errors.map { BeaconException.from(exception) }

                assertEquals(expected.map(Exception::toString).sorted(), errors.map(Throwable::toString).sorted())

                coVerify(exactly = requests.size) { messageController.onIncomingMessage(any(), any()) }
                verify(exactly = 0) { callback.onNewMessage(any()) }
                verify(exactly = requests.size) { callback.onError(any()) }

                confirmVerified(messageController, callback)
            }
        }
    }

    @Test
    fun `fails to respond when message sending failed`() {
        val error = IOException()
        coEvery { connectionController.send(any()) } returns Failure(error)

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

            beaconClient.respond(it, callback)
            runBlocking { testDeferred.await() }
        }

        assertEquals(expected.map(Throwable::toString).sorted(), exceptions.map(Throwable::toString).sorted())

        coVerify(exactly = responses.size) { messageController.onOutgoingMessage(any(), any(), any()) }
        coVerify(exactly = responses.size) { connectionController.send(any()) }

        verify(exactly = 0) { callback.onSuccess() }
        verify(exactly = responses.size) { callback.onError(any()) }

        confirmVerified(messageController, connectionController, callback)
    }
}