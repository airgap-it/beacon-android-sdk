package it.airgap.beaconsdk.internal.client

import createBroadcastBeaconRequest
import createBroadcastBeaconResponse
import createOperationBeaconRequest
import createOperationBeaconResponse
import createPermissionBeaconRequest
import createPermissionBeaconResponse
import createSignPayloadBeaconRequest
import createSignPayloadBeaconResponse
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.data.beacon.Origin
import it.airgap.beaconsdk.internal.controller.ConnectionController
import it.airgap.beaconsdk.internal.message.SerializedBeaconMessage
import it.airgap.beaconsdk.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.internal.serializer.Serializer
import it.airgap.beaconsdk.internal.serializer.provider.MockSerializerProvider
import it.airgap.beaconsdk.internal.transport.Transport
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.Success
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import takeHalf
import tryEmit
import kotlin.test.assertEquals

internal class ConnectionClientTest {

    @MockK
    private lateinit var transport: Transport

    private lateinit var serializer: Serializer
    private lateinit var connectionClient: ConnectionController

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        serializer = Serializer(MockSerializerProvider())
        connectionClient = ConnectionController(transport, serializer)
    }

    @Test
    fun `subscribes for new messages`() {
        val expected = beaconVersionedRequests.shuffled().takeHalf()
        val connectionMessages = validConnectionMessages(expected)

        val connectionMessageFlow =
            MutableSharedFlow<InternalResult<SerializedBeaconMessage>>(connectionMessages.size + 1)

        every { transport.subscribe() } answers { connectionMessageFlow }

        val messages = runBlocking {
            connectionClient.subscribe()
                .onStart { connectionMessageFlow.tryEmit(connectionMessages) }
                .mapNotNull { it.valueOrNull() }
                .take(connectionMessages.size)
                .toList()
        }

        assertEquals(expected.sortedBy { it.toString() }, messages.sortedBy { it.toString() })
    }

    @Test
    fun `sends serialized respond`() {
        coEvery { transport.send(any(), any()) } returns Success()

        val response = beaconVersionedResponses.shuffled().first()
        val serialized = serializer.serialize(response).value()

        runBlocking { connectionClient.send(response).value() }

        coVerify(exactly = 1) { transport.send(serialized) }
    }

    private val version: String = "2"
    private val senderId: String = "beaconId"

    private val beaconVersionedRequests: List<VersionedBeaconMessage> = listOf(
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, createPermissionBeaconRequest()),
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, createOperationBeaconRequest()),
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, createSignPayloadBeaconRequest()),
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, createBroadcastBeaconRequest()),
    )

    private val beaconVersionedResponses: List<VersionedBeaconMessage> = listOf(
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, createPermissionBeaconResponse()),
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, createOperationBeaconResponse()),
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, createSignPayloadBeaconResponse()),
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, createBroadcastBeaconResponse()),
    )

    private fun validConnectionMessages(beaconRequests: List<VersionedBeaconMessage>): List<SerializedBeaconMessage> =
        beaconRequests.map {
            SerializedBeaconMessage(Origin.P2P("1"), serializer.serialize(it).value())
        }

    private fun invalidConnectionMessages(n: Int): List<SerializedBeaconMessage> =
        (0 until n).map { SerializedBeaconMessage(Origin.P2P("1"), it.toString()) }

}