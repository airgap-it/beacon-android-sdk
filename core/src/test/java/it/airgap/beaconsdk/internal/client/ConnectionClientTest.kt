package it.airgap.beaconsdk.internal.client

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.data.network.Network
import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.data.sdk.Origin
import it.airgap.beaconsdk.data.tezos.TezosOperation
import it.airgap.beaconsdk.internal.data.ConnectionMessage
import it.airgap.beaconsdk.internal.serializer.Serializer
import it.airgap.beaconsdk.internal.serializer.provider.MockSerializerProvider
import it.airgap.beaconsdk.internal.transport.Transport
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.internalSuccess
import it.airgap.beaconsdk.message.BeaconMessage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class ConnectionClientTest {

    @MockK
    private lateinit var transport: Transport

    private lateinit var serializer: Serializer
    private lateinit var connectionClient: ConnectionClient

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        serializer = Serializer(MockSerializerProvider())
        connectionClient = ConnectionClient(transport, serializer)
    }

    @Test
    fun `subscribes for new messages`() {
        val expected = beaconRequests.shuffled().takeHalf()
        val connectionMessages = validConnectionMessages(expected)

        val connectionMessageFlow =
            MutableSharedFlow<InternalResult<ConnectionMessage>>(connectionMessages.size + 1)

        every { transport.subscribe() } answers { connectionMessageFlow }

        val messages = runBlocking {
            connectionClient.subscribe()
                .onStart { connectionMessageFlow.tryEmit(connectionMessages) }
                .mapNotNull { it.getOrNull() }
                .take(connectionMessages.size)
                .toList()
        }

        assertEquals(expected.sortedBy { it.toString() }, messages.sortedBy { it.toString() })
    }

    @Test
    fun `sends serialized respond`() {
        coEvery { transport.send(any(), any()) } returns internalSuccess(Unit)

        val response = beaconResponses.shuffled().first()
        val serialized = serializer.serialize(response).getOrThrow()

        runBlocking { connectionClient.send(response).getOrThrow() }

        coVerify(exactly = 1) { transport.send(serialized) }
    }

    private val beaconRequests: List<BeaconMessage.Request> = listOf(
        BeaconMessage.Request.Permission(
            "1",
            "1",
            "1",
            AppMetadata("1", "mockApp"),
            Network.Custom(),
            emptyList(),
        ),
        BeaconMessage.Request.Operation(
            "1",
            "1",
            "1",
            Network.Custom(),
            TezosOperation.DelegationOperation(
                "source",
                "fee",
                "counter",
                "gasLimit",
                "storageLimit",
                null,
            ),
            "sourceAddress",
        ),
        BeaconMessage.Request.SignPayload(
            "1",
            "1",
            "1",
            "payload",
            "sourceAddress",
        ),
        BeaconMessage.Request.Broadcast(
            "1",
            "1",
            "1",
            Network.Custom(),
            "signed",
        ),
    )

    private val beaconResponses: List<BeaconMessage.Response> = listOf(
        BeaconMessage.Response.Permission(
            "1",
            "1",
            "1",
            "publicKey",
            Network.Custom(),
            emptyList(),
        ),
        BeaconMessage.Response.Operation(
            "1",
            "1",
            "1",
            "transactionHash",
        ),
        BeaconMessage.Response.SignPayload(
            "1",
            "1",
            "1",
            "signature",
        ),
        BeaconMessage.Response.Broadcast(
            "1",
            "1",
            "1",
            "transactionHash",
        ),
    )

    private fun validConnectionMessages(beaconRequests: List<BeaconMessage.Request>): List<ConnectionMessage> =
        beaconRequests.map {
            ConnectionMessage(Origin.P2P("1"), serializer.serialize(it).getOrThrow())
        }

    private fun invalidConnectionMessages(n: Int): List<ConnectionMessage> =
        (0 until n).map { ConnectionMessage(Origin.P2P("1"), it.toString()) }

    private fun <T> MutableSharedFlow<InternalResult<T>>.tryEmit(messages: List<T>) {
        messages.forEach { tryEmit(internalSuccess(it)) }
    }

    private fun <T> List<T>.takeHalf(): List<T> = take(size / 2)
}