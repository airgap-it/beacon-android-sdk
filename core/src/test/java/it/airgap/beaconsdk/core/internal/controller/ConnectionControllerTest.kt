package it.airgap.beaconsdk.core.internal.controller

import androidx.annotation.IntRange
import beaconVersionedRequests
import beaconVersionedResponses
import connectionMessageFlow
import failures
import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.internal.compat.CoreCompat
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.message.*
import it.airgap.beaconsdk.core.internal.serializer.Serializer
import it.airgap.beaconsdk.core.internal.serializer.contextualJson
import it.airgap.beaconsdk.core.internal.serializer.provider.MockSerializerProvider
import it.airgap.beaconsdk.core.internal.transport.Transport
import it.airgap.beaconsdk.core.internal.utils.failure
import it.airgap.beaconsdk.core.internal.utils.success
import it.airgap.beaconsdk.core.scope.BeaconScope
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mockDependencyRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import tryEmitFailures
import tryEmitValues
import versionedBeaconMessageContext
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ConnectionControllerTest {

    @MockK
    private lateinit var transport: Transport

    private lateinit var dependencyRegistry: DependencyRegistry
    private lateinit var serializerProvider: MockSerializerProvider
    private lateinit var serializer: Serializer
    private lateinit var connectionClient: ConnectionController
    private lateinit var json: Json

    private val origin: Connection.Id = Connection.Id.P2P("id")
    private val beaconScope: BeaconScope = BeaconScope.Global

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { transport.type } returns Connection.Type.P2P

        dependencyRegistry = mockDependencyRegistry()
        every { dependencyRegistry.compat } returns CoreCompat(beaconScope)

        json = contextualJson(dependencyRegistry.blockchainRegistry, dependencyRegistry.compat)

        serializerProvider = MockSerializerProvider(json)
        serializer = Serializer(serializerProvider)
        connectionClient = ConnectionController(listOf(transport), serializer)
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `subscribes for new messages`() {
        val requests = beaconVersionedRequests(context = dependencyRegistry.versionedBeaconMessageContext).shuffled()
        val connectionMessages = validConnectionMessages(requests)
        val connectionMessageFlow = connectionMessageFlow(connectionMessages.size + 1)

        every { transport.subscribe() } answers { connectionMessageFlow }

        val messages = runBlocking {
            connectionClient.subscribe()
                .onStart { connectionMessageFlow.tryEmitValues(connectionMessages) }
                .mapNotNull { it.getOrNull() }
                .take(connectionMessages.size)
                .toList()
        }

        val expected = requests.map { BeaconIncomingConnectionMessage(origin, it) }

        assertEquals(expected.sortedBy { it.toString() }, messages.sortedBy { it.toString() })
    }

    @Test
    fun `propagates failure messages`() {
        val failures = failures<IncomingConnectionTransportMessage>(2, IllegalStateException())
        val connectionMessageFlow = connectionMessageFlow(failures.size + 1)

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
        val connectionMessageFlow = connectionMessageFlow(connectionMessages.size + 1)

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
        coEvery { transport.send(any()) } returns Result.success()

        val destination = Connection.Id.P2P("receiverId")
        val response = beaconVersionedResponses(context = dependencyRegistry.versionedBeaconMessageContext).shuffled().first()
        val serialized = serializer.serialize(response).getOrThrow()

        val message = BeaconOutgoingConnectionMessage(destination, response)
        val expected = SerializedOutgoingConnectionMessage(destination, serialized)

        runBlocking { connectionClient.send(message).getOrThrow() }

        coVerify(exactly = 1) { transport.send(expected) }
    }

    @Test
    fun `returns failure on respond when serialization failed`() {
        serializerProvider.shouldFail = true

        val response = beaconVersionedResponses(context = dependencyRegistry.versionedBeaconMessageContext).shuffled().first()
        val result = runBlocking { connectionClient.send(BeaconOutgoingConnectionMessage(Connection.Id.P2P("receiverId"), response)) }

        assertTrue(result.isFailure, "Expected result to be a failure")
        coVerify(exactly = 0) { transport.send(any()) }

        confirmVerified(transport)
    }

    @Test
    fun `returns failure on respond when sending failed`() {
        coEvery { transport.send(any()) } returns Result.failure()

        val response = beaconVersionedResponses(context = dependencyRegistry.versionedBeaconMessageContext).shuffled().first()
        val result = runBlocking { connectionClient.send(BeaconOutgoingConnectionMessage(Connection.Id.P2P("receiverId"), response)) }

        assertTrue(result.isFailure, "Expected result to be a failure")
        coVerify(exactly = 1) { transport.send(any()) }
    }

    private fun validConnectionMessages(beaconRequests: List<VersionedBeaconMessage>): List<IncomingConnectionTransportMessage> =
        beaconRequests.map { SerializedIncomingConnectionMessage(origin, serializer.serialize(it).getOrThrow()) }

    private fun invalidConnectionMessages(@IntRange(from = 1) number: Int = 1): List<IncomingConnectionTransportMessage> =
        (0 until number).map { SerializedIncomingConnectionMessage(origin, it.toString()) }
}