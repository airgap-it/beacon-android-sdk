package it.airgap.beaconsdk.compat.client

import beaconErrors
import beaconResponses
import beaconVersionedRequests
import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.client.BeaconClient
import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.exception.BeaconException
import it.airgap.beaconsdk.internal.controller.ConnectionController
import it.airgap.beaconsdk.internal.controller.MessageController
import it.airgap.beaconsdk.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.internal.storage.MockStorage
import it.airgap.beaconsdk.internal.storage.decorator.DecoratedExtendedStorage
import it.airgap.beaconsdk.internal.utils.Failure
import it.airgap.beaconsdk.internal.utils.Success
import it.airgap.beaconsdk.message.BeaconMessage
import it.airgap.beaconsdk.message.ErrorBeaconMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import tryEmit
import versionedBeaconMessage
import versionedBeaconMessageFlow
import versionedBeaconMessages
import kotlin.test.assertEquals

internal class BeaconClientTest {

    @MockK
    private lateinit var connectionController: ConnectionController

    @MockK
    private lateinit var messageController: MessageController

    private lateinit var storage: DecoratedExtendedStorage
    private lateinit var beaconClient: BeaconClient

    private lateinit var testDeferred: CompletableDeferred<Unit>

    private val appName: String = "mockApp"

    private val beaconId: String = "beaconId"

    private val dAppVersion: String = "2"
    private val dAppId: String = "dAppId"

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        coEvery { messageController.onIncomingMessage(any()) } coAnswers  {
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

        testDeferred = CompletableDeferred()
    }

    @Test
    fun `connects for messages with callback`() {
        runBlockingTest {
            val requests = beaconVersionedRequests().shuffled()
            val beaconMessageFlow = versionedBeaconMessageFlow(requests.size + 1)

            every { connectionController.subscribe() } answers { beaconMessageFlow }

            storage.addAppMetadata(listOf(AppMetadata(dAppId, "otherApp")))

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
                beaconMessageFlow.tryEmit(requests)

                testDeferred.await()

                val expected = requests.map { it.toBeaconMessage(storage) }

                assertEquals(expected.sortedBy { it.toString() }, messages.sortedBy { it.toString() })

                coVerify(exactly = expected.size) { messageController.onIncomingMessage(any()) }
                verify(exactly = requests.size) { callback.onNewMessage(any()) }
                verify(exactly = 0) { callback.onError(any()) }

                confirmVerified(messageController, callback)
            }
        }
    }

    @Test
    fun `responds to request`() {
        coEvery { connectionController.send(any()) } returns Success()

        val responses = beaconResponses().shuffled()
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

            val expected = versionedBeaconMessage(it, dAppVersion, beaconId)

            beaconClient.respond(it, callback)
            runBlocking { testDeferred.await() }

            coVerify { messageController.onOutgoingMessage(any(), it) }
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
            val beaconMessageFlow = versionedBeaconMessageFlow(requests.size + 1)

            val exception = Exception()

            every { connectionController.subscribe() } answers { beaconMessageFlow }
            coEvery { messageController.onIncomingMessage(any()) } returns Failure(exception)

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
                beaconMessageFlow.tryEmit(requests)

                testDeferred.await()

                val expected = errors.map { BeaconException.Internal(cause = exception) }

                assertEquals(expected.map(Exception::toString).sorted(), errors.map(Throwable::toString).sorted())

                coVerify(exactly = requests.size) { messageController.onIncomingMessage(any()) }
                verify(exactly = 0) { callback.onNewMessage(any()) }
                verify(exactly = requests.size) { callback.onError(any()) }

                confirmVerified(messageController, callback)
            }
        }
    }

    @Test
    fun `returns specific BeaconException when known error occurred`() {
        runBlockingTest {
            val errors = beaconErrors().shuffled()
            val beaconMessageFlow = versionedBeaconMessageFlow(errors.size + 1)

            every { connectionController.subscribe() } answers { beaconMessageFlow }

            runBlocking {
                val exceptions = mutableListOf<Throwable>()
                val callback = spyk<OnNewMessageListener>(
                    object : OnNewMessageListener {
                        override fun onNewMessage(message: BeaconMessage) = Unit

                        override fun onError(error: Throwable) {
                            exceptions.add(error)

                            if (exceptions.size == errors.size) testDeferred.complete(Unit)
                        }
                    }
                )

                beaconClient.connect(callback)
                beaconMessageFlow.tryEmit(versionedBeaconMessages(errors))

                testDeferred.await()

                val expected = errors.map { BeaconException.fromType(it.errorType) }

                assertEquals(expected.map(Exception::toString).sorted(), exceptions.map(Throwable::toString).sorted())

                coVerify(exactly = errors.size) { messageController.onIncomingMessage(any()) }
                verify(exactly = 0) { callback.onNewMessage(any()) }
                verify(exactly = errors.size) { callback.onError(any()) }

                confirmVerified(messageController, callback)
            }
        }
    }

    @Test
    fun `fails to respond when outgoing message processing failed with known Beacon error`() {
        val errors = beaconErrors().shuffled()
        val responses = beaconResponses().shuffled()

        val expected = mutableListOf<Throwable>()
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

        errors.forEach { error ->
            val beaconException = BeaconException.fromType(error.errorType)
            coEvery { messageController.onOutgoingMessage(any(), any()) } returns Failure(beaconException)

            responses.forEach { response ->
                expected.add(beaconException)
                testDeferred = CompletableDeferred()

                beaconClient.respond(response, callback)
                runBlocking { testDeferred.await() }
            }
        }

        assertEquals(expected.map(Throwable::toString).sorted(), exceptions.map(Throwable::toString).sorted())

        coVerify(exactly = responses.size * errors.size) { messageController.onOutgoingMessage(any(), any()) }
        coVerify(exactly = 0) { connectionController.send(any()) }

        verify(exactly = 0) { callback.onSuccess() }
        verify(exactly = responses.size * errors.size) { callback.onError(any()) }

        confirmVerified(messageController, connectionController, callback)
    }

    @Test
    fun `fails to respond when message sending failed`() {
        coEvery { connectionController.send(any()) } returns Failure()

        val responses = beaconResponses().shuffled()

        val expected = responses.map { BeaconException.Internal() }
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

        coVerify(exactly = responses.size) { messageController.onOutgoingMessage(any(), any()) }
        coVerify(exactly = responses.size) { connectionController.send(any()) }

        verify(exactly = 0) { callback.onSuccess() }
        verify(exactly = responses.size) { callback.onError(any()) }

        confirmVerified(messageController, connectionController, callback)
    }
}