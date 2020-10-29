package it.airgap.beaconsdk.client

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
import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.internal.controller.ConnectionController
import it.airgap.beaconsdk.internal.controller.MessageController
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.internal.storage.MockBeaconStorage
import it.airgap.beaconsdk.internal.storage.decorator.DecoratedExtendedStorage
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.Success
import it.airgap.beaconsdk.message.BeaconResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import takeHalf
import tryEmit
import kotlin.test.assertEquals

internal class BeaconClientTest {

    @MockK
    private lateinit var connectionClient: ConnectionController

    @MockK
    private lateinit var messageController: MessageController

    @MockK
    private lateinit var crypto: Crypto

    private lateinit var storage: DecoratedExtendedStorage
    private lateinit var beaconClient: BeaconClient

    private lateinit var testDeferred: CompletableDeferred<Unit>

    private val version: String = "2"
    private val beaconId: String = "beaconId"

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { crypto.generateRandomSeed() } returns Success(secretSeed)
        every { crypto.getKeyPairFromSeed(any()) } returns Success(KeyPair(privateKey, publicKey))

        coEvery { messageController.onIncomingMessage(any()) } coAnswers  {
            Success(firstArg<VersionedBeaconMessage>().toBeaconMessage(storage))
        }
        coEvery { messageController.onOutgoingMessage(any(), any()) } coAnswers {
            Success(VersionedBeaconMessage.fromBeaconMessage(version, beaconId, secondArg()))
        }

        storage = DecoratedExtendedStorage(MockBeaconStorage())

        beaconClient = BeaconClient("mockApp", beaconId, connectionClient, messageController, storage)

        testDeferred = CompletableDeferred()
    }

    @Test
    fun `connects for messages flow`() {
        val requests = beaconVersionedRequests.shuffled().takeHalf()
        val beaconMessageFlow =
            MutableSharedFlow<InternalResult<VersionedBeaconMessage>>(requests.size + 1)
        every { connectionClient.subscribe() } answers { beaconMessageFlow }

        val appMetadata = AppMetadata(senderId, "mockApp")
        runBlocking { storage.addAppsMetadata(appMetadata) }

        val messages = runBlocking {
            beaconClient.connect()
                .onStart { beaconMessageFlow.tryEmit(requests) }
                .mapNotNull { it.getOrNull() }
                .take(requests.size)
                .toList()
        }

        val expected = runBlocking { requests.map { it.toBeaconMessage(storage) } }

        assertEquals(expected.sortedBy { it.toString() }, messages.sortedBy { it.toString() })
        coVerify(exactly = expected.size) { messageController.onIncomingMessage(any()) }
    }

    @Test
    fun `responds to request with suspend fun`() {
        coEvery { connectionClient.send(any()) } returns Success()

        val response = beaconResponses.shuffled().first()
        val internalResponse = VersionedBeaconMessage.fromBeaconMessage(version, beaconId, response)

        runBlocking { beaconClient.respond(response) }

        coVerify { connectionClient.send(internalResponse) }
    }

    private val senderId: String = "1"
    private val secretSeed: String = "seed"
    private val privateKey: ByteArray = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
    private val publicKey: ByteArray = byteArrayOf(10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)

    private val beaconVersionedRequests: List<VersionedBeaconMessage> = listOf(
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, createPermissionBeaconRequest(senderId = senderId)),
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, createOperationBeaconRequest(senderId = senderId)),
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, createSignPayloadBeaconRequest(senderId = senderId)),
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, createBroadcastBeaconRequest(senderId = senderId,)),
    )

    private val beaconResponses: List<BeaconResponse> = listOf(
        createPermissionBeaconResponse(),
        createOperationBeaconResponse(),
        createSignPayloadBeaconResponse(),
        createBroadcastBeaconResponse(),
    )
}