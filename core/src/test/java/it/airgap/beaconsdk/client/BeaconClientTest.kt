package it.airgap.beaconsdk.client

import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.data.network.Network
import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.data.tezos.TezosOperation
import it.airgap.beaconsdk.internal.client.ConnectionClient
import it.airgap.beaconsdk.internal.controller.MessageController
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.storage.ExtendedStorage
import it.airgap.beaconsdk.internal.utils.internalSuccess
import it.airgap.beaconsdk.message.BeaconMessage
import it.airgap.beaconsdk.storage.MockBeaconStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class BeaconClientTest {

    @MockK
    private lateinit var connectionClient: ConnectionClient

    @MockK
    private lateinit var messageController: MessageController

    @MockK
    private lateinit var crypto: Crypto

    private lateinit var storage: ExtendedStorage
    private lateinit var beaconClient: BeaconClient

    private lateinit var testDeferred: CompletableDeferred<Unit>

    private val beaconId: String = "beaconId"

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { crypto.generateRandomSeed() } returns internalSuccess(secretSeed)
        every { crypto.getKeyPairFromSeed(any()) } returns internalSuccess(KeyPair(privateKey, publicKey))

        coEvery { messageController.onRequest(any()) } returns internalSuccess(Unit)
        coEvery { messageController.onResponse(any()) } returns internalSuccess(Unit)

        storage = ExtendedStorage(MockBeaconStorage())

        beaconClient = BeaconClient("mockApp", beaconId, connectionClient, messageController, storage)

        testDeferred = CompletableDeferred()
    }

    @Test
    fun `connects for messages flow`() {
        val requests = beaconRequests.shuffled().takeHalf()
        val beaconRequestFlow =
            MutableSharedFlow<InternalResult<BeaconMessage.Request>>(requests.size + 1)
        every { connectionClient.subscribe() } answers { beaconRequestFlow }

        val appMetadata = AppMetadata(senderId, "mockApp")
        runBlocking { storage.addAppsMetadata(appMetadata) }

        val messages = runBlocking {
            beaconClient.connect()
                .onStart { beaconRequestFlow.tryEmit(requests) }
                .mapNotNull { it.getOrNull() }
                .take(requests.size)
                .toList()
        }

        val expected = requests.map { request ->
            request.also { it.extendWithMetadata(appMetadata) }
        }

        assertEquals(expected.sortedBy { it.toString() }, messages.sortedBy { it.toString() })
        coVerify(exactly = expected.size) { messageController.onRequest(any()) }
    }

    @Test
    fun `responds to request with suspend fun`() {
        coEvery { connectionClient.send(any()) } returns internalSuccess(Unit)

        val response = beaconResponses.shuffled().first()
        val internalResponse = response.apply { senderId = beaconId }

        runBlocking { beaconClient.respond(response) }

        coVerify { connectionClient.send(internalResponse) }
    }

    private val senderId: String = "1"
    private val secretSeed: String = "seed"
    private val privateKey: ByteArray = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
    private val publicKey: ByteArray = byteArrayOf(10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)

    private val beaconRequests: List<BeaconMessage.Request> = listOf(
        BeaconMessage.Request.Permission(
            "1",
            "1",
            senderId,
            AppMetadata(senderId, "mockApp"),
            Network.Custom(),
            emptyList()
        ),
        BeaconMessage.Request.Operation(
            "1",
            "1",
            senderId,
            Network.Custom(),
            TezosOperation.Delegation(
                "source",
                "fee",
                "counter",
                "gasLimit",
                "storageLimit",
                null
            ),
            "sourceAddress"
        ),
        BeaconMessage.Request.SignPayload(
            "1",
            "1",
            senderId,
            "payload",
            "sourceAddress"
        ),
        BeaconMessage.Request.Broadcast(
            "1",
            "1",
            senderId,
            Network.Custom(),
            "signed"
        ),
    )

    private val beaconResponses: List<BeaconMessage.Response> = listOf(
        BeaconMessage.Response.Permission(
            "1",
            "publicKey",
            Network.Custom(),
            emptyList(),
        ),
        BeaconMessage.Response.Operation(
            "1",
            "transactionHash",
        ),
        BeaconMessage.Response.SignPayload(
            "1",
            "signature",
        ),
        BeaconMessage.Response.Broadcast(
            "1",
            "transactionHash",
        ),
    )

    private fun <T> MutableSharedFlow<InternalResult<T>>.tryEmit(messages: List<T>) {
        messages.forEach { tryEmit(internalSuccess(it)) }
    }

    private fun <T> List<T>.takeHalf(): List<T> = take(size / 2)
}