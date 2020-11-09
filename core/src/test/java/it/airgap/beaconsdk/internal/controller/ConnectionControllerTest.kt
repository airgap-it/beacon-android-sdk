package it.airgap.beaconsdk.internal.controller

import androidx.annotation.IntRange
import beaconVersionedRequests
import beaconVersionedResponses
import failures
import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.data.beacon.Origin
import it.airgap.beaconsdk.internal.message.ConnectionTransportMessage
import it.airgap.beaconsdk.internal.message.BeaconConnectionMessage
import it.airgap.beaconsdk.internal.message.SerializedConnectionMessage
import it.airgap.beaconsdk.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.internal.serializer.Serializer
import it.airgap.beaconsdk.internal.serializer.provider.MockSerializerProvider
import it.airgap.beaconsdk.internal.transport.Transport
import it.airgap.beaconsdk.internal.utils.Failure
import it.airgap.beaconsdk.internal.utils.Success
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import originatedMessageFlow
import tryEmitFailures
import tryEmitValues
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ConnectionControllerTest {

    @MockK
    private lateinit var transport: Transport

    private lateinit var serializerProvider: MockSerializerProvider
    private lateinit var serializer: Serializer
    private lateinit var connectionClient: ConnectionController

    private val origin: Origin = Origin.P2P("id")

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        serializerProvider = MockSerializerProvider()
        serializer = Serializer(serializerProvider)
        connectionClient = ConnectionController(transport, serializer)
    }

    @Test
    fun `subscribes for new messages`() {
        val requests = beaconVersionedRequests().shuffled()
        val connectionMessages = validConnectionMessages(requests)
        val connectionMessageFlow = originatedMessageFlow(connectionMessages.size + 1)

        every { transport.subscribe() } answers { connectionMessageFlow }

        val messages = runBlocking {
            connectionClient.subscribe()
                .onStart { connectionMessageFlow.tryEmitValues(connectionMessages) }
                .mapNotNull { it.valueOrNull() }
                .take(connectionMessages.size)
                .toList()
        }

        val expected = requests.map { BeaconConnectionMessage(origin, it) }

        assertEquals(expected.sortedBy { it.toString() }, messages.sortedBy { it.toString() })
    }

    @Test
    fun `propagates failure messages`() {
        val failures = failures<ConnectionTransportMessage>(2, IllegalStateException())
        val connectionMessageFlow = originatedMessageFlow(failures.size + 1)

        every { transport.subscribe() } answers { connectionMessageFlow }

        val exceptions = runBlocking {
            connectionClient.subscribe()
                .onStart { connectionMessageFlow.tryEmitFailures(failures) }
                .take(failures.size)
                .toList()
        }

        assertEquals(failures.map { it.toString() }.sorted(), exceptions.map { it.toString() }.sorted())
    }

    @Test
    fun `emits failure message if deserialization failed`() {
        serializerProvider.shouldFail = true

        val connectionMessages = invalidConnectionMessages(2)
        val connectionMessageFlow = originatedMessageFlow(connectionMessages.size + 1)

        every { transport.subscribe() } answers { connectionMessageFlow }

        val messages = runBlocking {
            connectionClient.subscribe()
                .onStart { connectionMessageFlow.tryEmitValues(connectionMessages) }
                .take(connectionMessages.size)
                .toList()
        }

        assertTrue(messages.all { it.isFailure }, "Expected all messages to be a failure")
    }

    @Test
    fun `sends serialized respond`() {
        coEvery { transport.send(any(), any()) } returns Success()

        val response = beaconVersionedResponses().shuffled().first()
        val serialized = serializer.serialize(response).value()

        runBlocking { connectionClient.send(response).value() }

        coVerify(exactly = 1) { transport.send(serialized) }

        confirmVerified(transport)
    }

    @Test
    fun `returns failure on respond when serialization failed`() {
        serializerProvider.shouldFail = true

        val response = beaconVersionedResponses().shuffled().first()
        val result = runBlocking { connectionClient.send(response) }

        assertTrue(result.isFailure, "Expected result to be a failure")
        coVerify(exactly = 0) { transport.send(any(), any()) }

        confirmVerified(transport)
    }

    @Test
    fun `returns failure on respond when sending failed`() {
        coEvery { transport.send(any(), any()) } returns Failure()

        val response = beaconVersionedResponses().shuffled().first()
        val result = runBlocking { connectionClient.send(response) }

        assertTrue(result.isFailure, "Expected result to be a failure")
        coVerify(exactly = 1) { transport.send(any()) }

        confirmVerified(transport)
    }

    private fun validConnectionMessages(beaconRequests: List<VersionedBeaconMessage>): List<ConnectionTransportMessage> =
        beaconRequests.map { SerializedConnectionMessage(origin, serializer.serialize(it).value()) }

    private fun invalidConnectionMessages(@IntRange(from = 1) number: Int = 1): List<ConnectionTransportMessage> =
        (0 until number).map { SerializedConnectionMessage(origin, it.toString()) }
}