package it.airgap.beaconsdk.compat.client

import createBroadcastBeaconRequest
import createBroadcastBeaconResponse
import createOperationBeaconRequest
import createOperationBeaconResponse
import createPermissionBeaconRequest
import createPermissionBeaconResponse
import createSignPayloadBeaconRequest
import createSignPayloadBeaconResponse
import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.client.BeaconClient
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
import it.airgap.beaconsdk.message.BeaconMessage
import it.airgap.beaconsdk.message.BeaconResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
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
    fun `connects for messages with callback`() {
        val requests = beaconVersionedRequests.shuffled().takeHalf()
        val beaconRequestFlow =
            MutableSharedFlow<InternalResult<VersionedBeaconMessage>>(requests.size + 1)
        every { connectionClient.subscribe() } answers { beaconRequestFlow }

        val appMetadata = AppMetadata(senderId, "mockApp")
        runBlocking { storage.addAppsMetadata(appMetadata) }
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
            beaconRequestFlow.tryEmit(requests)

            testDeferred.await()

            val expected = requests.map { it.toBeaconMessage(storage) }

            assertEquals(expected.sortedBy { it.toString() }, messages.sortedBy { it.toString() })
            coVerify(exactly = expected.size) { messageController.onIncomingMessage(any()) }
            verify(exactly = requests.size) { callback.onNewMessage(any()) }
            verify(exactly = 0) { callback.onError(any()) }
        }
    }

    @Test
    fun `responds to request with callback`() {
        coEvery { connectionClient.send(any()) } returns Success()

        val response = beaconResponses.shuffled().first()
        val internalResponse = VersionedBeaconMessage.fromBeaconMessage(version, beaconId, response)

        val callback = spyk<ResponseCallback>(object : ResponseCallback {
            override fun onSuccess() {
                testDeferred.complete(Unit)
            }

            override fun onError(error: Throwable) {
                testDeferred.complete(Unit)
            }
        })

        beaconClient.respond(response, callback)

        runBlocking { testDeferred.await() }

        coVerify { connectionClient.send(internalResponse) }
        verify(exactly = 1) { callback.onSuccess() }
        verify(exactly = 0) { callback.onError(any()) }
    }

    private val senderId: String = "1"
    private val secretSeed: String = "seed"
    private val privateKey: ByteArray = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
    private val publicKey: ByteArray = byteArrayOf(10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)

    private val beaconVersionedRequests: List<VersionedBeaconMessage> = listOf(
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, createPermissionBeaconRequest(senderId = senderId)),
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, createOperationBeaconRequest(senderId = senderId)),
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, createSignPayloadBeaconRequest(senderId = senderId)),
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, createBroadcastBeaconRequest(senderId = senderId)),
    )

    private val beaconResponses: List<BeaconResponse> = listOf(
        createPermissionBeaconResponse(),
        createOperationBeaconResponse(),
        createSignPayloadBeaconResponse(),
        createBroadcastBeaconResponse(),
    )
}