package it.airgap.beaconsdk.internal.controller

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import it.airgap.beaconsdk.data.network.Network
import it.airgap.beaconsdk.data.permission.PermissionInfo
import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.data.tezos.TezosOperation
import it.airgap.beaconsdk.internal.message.beaconmessage.ApiBeaconMessage
import it.airgap.beaconsdk.internal.protocol.Protocol
import it.airgap.beaconsdk.internal.protocol.ProtocolRegistry
import it.airgap.beaconsdk.internal.storage.ExtendedStorage
import it.airgap.beaconsdk.internal.storage.Storage
import it.airgap.beaconsdk.internal.utils.AccountUtils
import it.airgap.beaconsdk.internal.utils.currentTimestamp
import it.airgap.beaconsdk.internal.utils.internalSuccess
import it.airgap.beaconsdk.storage.MockBeaconStorageKtx
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MessageControllerTest {

    @MockK
    private lateinit var protocol: Protocol

    @MockK
    private lateinit var protocolRegistry: ProtocolRegistry

    @MockK
    private lateinit var accountUtils: AccountUtils

    private lateinit var storage: ExtendedStorage
    private lateinit var messageController: MessageController

    private val currentTimeMillis: Long = 1

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockkStatic("it.airgap.beaconsdk.internal.utils.TimeKt")
        every { currentTimestamp() } returns currentTimeMillis

        every { protocol.getAddressFromPublicKey(any()) } answers { internalSuccess(firstArg()) }
        every { protocolRegistry.get(any()) } returns protocol

        every { accountUtils.getAccountIdentifier(any(), any()) } answers { internalSuccess(firstArg()) }

        storage = ExtendedStorage(Storage.KtxDecorator(MockBeaconStorageKtx()))
        messageController = MessageController(protocolRegistry, storage, accountUtils)
    }

    @Test
    fun `saves app metadata on permission request`() {
        val permissionRequest = beaconRequest<ApiBeaconMessage.Request.Permission>()

        runBlocking { messageController.onRequest(permissionRequest) }
        val appsMetadata = runBlocking { storage.getAppsMetadata() }

        assertEquals(listOf(permissionRequest.appMetadata), appsMetadata)
    }

    @Test
    fun `fails when processing response without matching pending request`() {
        val response = beaconResponses.shuffled().first()

        assertFailsWith(IllegalArgumentException::class) {
            runBlocking { messageController.onResponse(response).getOrThrow() }
        }
    }

    @Test
    fun `saves permissions on permission response`() {
        val permissionRequest = beaconRequest<ApiBeaconMessage.Request.Permission>()
        val permissionResponse = beaconResponse<ApiBeaconMessage.Response.Permission>()

        runBlocking {
            messageController.onRequest(permissionRequest)
            messageController.onResponse(permissionResponse)
        }

        val appMetadata = runBlocking { storage.getAppsMetadata().first() }
        val permissions = runBlocking { storage.getPermissions() }
        val expected = listOf(
            PermissionInfo(
                permissionResponse.publicKey,
                permissionResponse.publicKey,
                permissionResponse.network,
                permissionResponse.scopes,
                permissionResponse.senderId,
                appMetadata,
                "",
                permissionResponse.publicKey,
                currentTimeMillis
            )
        )

        assertEquals(expected, permissions)
    }

    private val messageId: String = "1"
    private val beaconRequests: List<ApiBeaconMessage.Request> = listOf(
        ApiBeaconMessage.Request.Permission(
            "1",
            messageId,
            "1",
            AppMetadata("1", "mockApp"),
            Network.Custom(),
            emptyList(),
        ),
        ApiBeaconMessage.Request.Operation(
            "1",
            messageId,
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
        ApiBeaconMessage.Request.SignPayload(
            "1",
            messageId,
            "1",
            "payload",
            "sourceAddress",
        ),
        ApiBeaconMessage.Request.Broadcast(
            "1",
            messageId,
            "1",
            Network.Custom(),
            "signed",
        ),
    )

    private val beaconResponses: List<ApiBeaconMessage.Response> = listOf(
        ApiBeaconMessage.Response.Permission(
            "1",
            messageId,
            "1",
            "publicKey",
            Network.Custom(),
            emptyList(),
        ),
        ApiBeaconMessage.Response.Operation(
            "1",
            messageId,
            "1",
            "transactionHash",
        ),
        ApiBeaconMessage.Response.SignPayload(
            "1",
            messageId,
            "1",
            "signature",
        ),
        ApiBeaconMessage.Response.Broadcast(
            "1",
            messageId,
            "1",
            "transactionHash",
        ),
    )

    private inline fun <reified T : ApiBeaconMessage.Request> beaconRequest(): T = beaconRequests.filterIsInstance<T>().first()
    private inline fun <reified T : ApiBeaconMessage.Response> beaconResponse(): T = beaconResponses.filterIsInstance<T>().first()
}