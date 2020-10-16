package it.airgap.beaconsdk.client

import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.data.network.Network
import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.data.tezos.TezosOperation
import it.airgap.beaconsdk.internal.client.ConnectionClient
import it.airgap.beaconsdk.internal.client.SdkClient
import it.airgap.beaconsdk.internal.controller.MessageController
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.message.beaconmessage.ApiBeaconMessage
import it.airgap.beaconsdk.internal.storage.ExtendedStorage
import it.airgap.beaconsdk.internal.utils.internalSuccess
import it.airgap.beaconsdk.internal.utils.uninitializedMessage
import it.airgap.beaconsdk.message.BeaconMessage
import it.airgap.beaconsdk.storage.MockBeaconStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BeaconWalletClientTest {

    @MockK
    private lateinit var connectionClient: ConnectionClient

    @MockK
    private lateinit var messageController: MessageController

    @MockK
    private lateinit var crypto: Crypto

    private lateinit var storage: ExtendedStorage
    private lateinit var sdkClient: SdkClient
    private lateinit var beaconWalletClient: BeaconWalletClient

    private lateinit var testDeferred: CompletableDeferred<Unit>

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { crypto.generateRandomSeed() } returns internalSuccess(secretSeed)
        every { crypto.getKeyPairFromSeed(any()) } returns internalSuccess(KeyPair(privateKey, publicKey))

        coEvery { messageController.onRequest(any()) } returns internalSuccess(Unit)
        coEvery { messageController.onResponse(any()) } returns internalSuccess(Unit)

        storage = ExtendedStorage(MockBeaconStorage())
        sdkClient = spyk(SdkClient(storage, crypto))

        beaconWalletClient = BeaconWalletClient("mockApp", sdkClient, connectionClient, messageController, storage)

        testDeferred = CompletableDeferred()
    }

    @Test
    fun `initializes itself with suspend fun`() {
        runBlocking { beaconWalletClient.init() }
        assertTrue(beaconWalletClient.isInitialized)
    }

    @Test
    fun `initializes itself with callback`() {
        val callback = spyk<BeaconClient.InitCallback>(object : BeaconClient.InitCallback {
            override fun onSuccess() {
                testDeferred.complete(Unit)
            }

            override fun onError(error: Throwable) {
                testDeferred.complete(Unit)
            }
        })

        beaconWalletClient.init(callback)
        runBlocking { testDeferred.await() }

        verify { callback.onSuccess() }
        assertTrue(beaconWalletClient.isInitialized)
        assertEquals("0a0b0c0d0e0f1011121314", beaconWalletClient.beaconId)
    }

    @Test
    fun `fails when interacting before initialization`() {
        assertFailsWith(IllegalStateException::class, uninitializedMessage(BeaconWalletClient.TAG)) {
            beaconWalletClient.beaconId
        }

        assertFailsWith(IllegalStateException::class, uninitializedMessage(BeaconWalletClient.TAG)) {
            runBlocking { beaconWalletClient.connect() }
        }

        assertFailsWith(IllegalStateException::class, uninitializedMessage(BeaconWalletClient.TAG)) {
            beaconWalletClient.connect(object : BeaconClient.OnNewMessageListener {
                override fun onNewMessage(message: BeaconMessage.Request) = Unit
                override fun onError(error: Throwable) = Unit
            })
        }

        assertFailsWith(IllegalStateException::class, uninitializedMessage(BeaconWalletClient.TAG)) {
            runBlocking { beaconWalletClient.respond(beaconResponses.shuffled().first()) }
        }

        assertFailsWith(IllegalStateException::class, uninitializedMessage(BeaconWalletClient.TAG)) {
            beaconWalletClient.respond(beaconResponses.shuffled().first(), object : BeaconClient.ResponseCallback {
                override fun onSuccess() = Unit
                override fun onError(error: Throwable) = Unit
            })
        }
    }

    @Test
    fun `connects for messages flow`() {
        val requests = beaconRequests.shuffled().takeHalf()
        val beaconRequestFlow =
            MutableSharedFlow<InternalResult<ApiBeaconMessage.Request>>(requests.size + 1)
        every { connectionClient.subscribe() } answers { beaconRequestFlow }

        val appMetadata = AppMetadata(senderId, "mockApp")
        runBlocking { storage.addAppsMetadata(appMetadata) }
        runBlocking { beaconWalletClient.init() }

        val messages = runBlocking {
            beaconWalletClient.connect()
                .onStart { beaconRequestFlow.tryEmit(requests) }
                .mapNotNull { it.getOrNull() }
                .take(requests.size)
                .toList()
        }

        val expected = requests.map { BeaconMessage.fromInternalBeaconRequest(it, appMetadata) }

        assertEquals(expected.sortedBy { it.toString() }, messages.sortedBy { it.toString() })
        coVerify(exactly = expected.size) { messageController.onRequest(any()) }
    }

    @Test
    fun `connects for messages with callback`() {
        val requests = beaconRequests.shuffled().takeHalf()
        val beaconRequestFlow =
            MutableSharedFlow<InternalResult<ApiBeaconMessage.Request>>(requests.size + 1)
        every { connectionClient.subscribe() } answers { beaconRequestFlow }

        val appMetadata = AppMetadata(senderId, "mockApp")
        runBlocking { storage.addAppsMetadata(appMetadata) }
        runBlocking { beaconWalletClient.init() }
        runBlocking {
            val messages = mutableListOf<BeaconMessage.Request>()
            val callback = spyk<BeaconClient.OnNewMessageListener>(
                object : BeaconClient.OnNewMessageListener {
                    override fun onNewMessage(message: BeaconMessage.Request) {
                        messages.add(message)

                        if (messages.size == requests.size) testDeferred.complete(Unit)
                    }

                    override fun onError(error: Throwable) = Unit
                }
            )
            beaconWalletClient.connect(callback)
            beaconRequestFlow.tryEmit(requests)

            testDeferred.await()

            val expected = requests.map { BeaconMessage.fromInternalBeaconRequest(it, appMetadata) }

            assertEquals(expected.sortedBy { it.toString() }, messages.sortedBy { it.toString() })
            coVerify(exactly = expected.size) { messageController.onRequest(any()) }
            verify(exactly = requests.size) { callback.onNewMessage(any()) }
            verify(exactly = 0) { callback.onError(any()) }
        }
    }

    @Test
    fun `responds to request with suspend fun`() {
        coEvery { connectionClient.send(any()) } returns internalSuccess(Unit)

        runBlocking { beaconWalletClient.init() }

        val response = beaconResponses.shuffled().first()
        val internalResponse = ApiBeaconMessage.fromBeaconResponse(response, sdkClient.beaconId!!)

        runBlocking { beaconWalletClient.respond(response) }

        coVerify { connectionClient.send(internalResponse) }
    }

    @Test
    fun `responds to request with callback`() {
        coEvery { connectionClient.send(any()) } returns internalSuccess(Unit)

        runBlocking { beaconWalletClient.init() }

        val response = beaconResponses.shuffled().first()
        val internalResponse = ApiBeaconMessage.fromBeaconResponse(response, sdkClient.beaconId!!)

        val callback = spyk<BeaconClient.ResponseCallback>(object : BeaconClient.ResponseCallback {
            override fun onSuccess() {
                testDeferred.complete(Unit)
            }

            override fun onError(error: Throwable) {
                testDeferred.complete(Unit)
            }
        })

        beaconWalletClient.respond(response, callback)

        runBlocking { testDeferred.await() }

        coVerify { connectionClient.send(internalResponse) }
        verify(exactly = 1) { callback.onSuccess() }
        verify(exactly = 0) { callback.onError(any()) }
    }

    private val senderId: String = "1"
    private val secretSeed: String = "seed"
    private val privateKey: ByteArray = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
    private val publicKey: ByteArray = byteArrayOf(10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)

    private val beaconRequests: List<ApiBeaconMessage.Request> = listOf(
        ApiBeaconMessage.Request.Permission(
            "1",
            "1",
            senderId,
            AppMetadata(senderId, "mockApp"),
            Network.Custom(),
            emptyList()
        ),
        ApiBeaconMessage.Request.Operation(
            "1",
            "1",
            senderId,
            Network.Custom(),
            TezosOperation.DelegationOperation(
                "source",
                "fee",
                "counter",
                "gasLimit",
                "storageLimit",
                null
            ),
            "sourceAddress"
        ),
        ApiBeaconMessage.Request.SignPayload(
            "1",
            "1",
            senderId,
            "payload",
            "sourceAddress"
        ),
        ApiBeaconMessage.Request.Broadcast(
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