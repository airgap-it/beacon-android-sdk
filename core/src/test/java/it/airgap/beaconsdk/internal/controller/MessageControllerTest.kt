package it.airgap.beaconsdk.internal.controller

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.Network
import it.airgap.beaconsdk.data.beacon.PermissionInfo
import it.airgap.beaconsdk.data.tezos.TezosOperation
import it.airgap.beaconsdk.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.internal.protocol.Protocol
import it.airgap.beaconsdk.internal.protocol.ProtocolRegistry
import it.airgap.beaconsdk.internal.storage.MockBeaconStorage
import it.airgap.beaconsdk.internal.storage.decorator.DecoratedExtendedStorage
import it.airgap.beaconsdk.internal.utils.AccountUtils
import it.airgap.beaconsdk.internal.utils.Success
import it.airgap.beaconsdk.internal.utils.currentTimestamp
import it.airgap.beaconsdk.message.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class MessageControllerTest {

    @MockK
    private lateinit var protocol: Protocol

    @MockK
    private lateinit var protocolRegistry: ProtocolRegistry

    @MockK
    private lateinit var accountUtils: AccountUtils

    private lateinit var storage: DecoratedExtendedStorage
    private lateinit var messageController: MessageController

    private val currentTimeMillis: Long = 1

    private val version: String = "2"
    private val senderId: String = "senderId"

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockkStatic("it.airgap.beaconsdk.internal.utils.TimeKt")
        every { currentTimestamp() } returns currentTimeMillis

        every { protocol.getAddressFromPublicKey(any()) } answers { Success(firstArg()) }
        every { protocolRegistry.get(any()) } returns protocol

        every { accountUtils.getAccountIdentifier(any(), any()) } answers { Success(firstArg()) }

        storage = DecoratedExtendedStorage(MockBeaconStorage())
        messageController = MessageController(protocolRegistry, storage, accountUtils)
    }

    @Test
    fun `saves app metadata on permission request`() {
        val permissionRequest = beaconVersionedRequest<PermissionBeaconRequest>()
        val versionedRequest = VersionedBeaconMessage.fromBeaconMessage(version, senderId, permissionRequest)

        runBlocking { messageController.onIncomingMessage(versionedRequest) }
        val appsMetadata = runBlocking { storage.getAppsMetadata() }

        assertEquals(listOf(permissionRequest.appMetadata), appsMetadata)
    }

    @Test
    fun `fails when processing response without matching pending request`() {
        val response = beaconResponses.shuffled().first()

        assertFailsWith(IllegalArgumentException::class) {
            runBlocking { messageController.onOutgoingMessage(senderId, response).value() }
        }
    }

    @Test
    fun `saves permissions on permission response`() {
        val permissionRequest = beaconVersionedRequest<PermissionBeaconRequest>()
        val permissionResponse = beaconResponse<PermissionBeaconResponse>()

        val versionedRequest = VersionedBeaconMessage.fromBeaconMessage(version, senderId, permissionRequest)

        runBlocking {
            messageController.onIncomingMessage(versionedRequest)
            messageController.onOutgoingMessage(senderId, permissionResponse)
        }

        val appMetadata = runBlocking { storage.getAppsMetadata().first() }
        val permissions = runBlocking { storage.getPermissions() }
        val expected = listOf(
            PermissionInfo(
                permissionResponse.publicKey,
                permissionResponse.publicKey,
                permissionResponse.network,
                permissionResponse.scopes,
                appMetadata.senderId,
                appMetadata,
                "",
                permissionResponse.publicKey,
                currentTimeMillis
            )
        )

        assertEquals(expected, permissions)
    }

    private val messageId: String = "1"
    private val beaconRequests: List<BeaconRequest> = listOf(
        PermissionBeaconRequest(
            "1",
            messageId,
            AppMetadata(senderId, "mockApp"),
            Network.Custom(),
            emptyList(),
        ),
        OperationBeaconRequest(
            "1",
            messageId,
            null,
            Network.Custom(),
            TezosOperation.Delegation(
                "source",
                "fee",
                "counter",
                "gasLimit",
                "storageLimit",
                null,
            ),
            "sourceAddress",
        ),
        SignPayloadBeaconRequest(
            "1",
            messageId,
            null,
            "payload",
            "sourceAddress",
        ),
        BroadcastBeaconRequest(
            "1",
            messageId,
            null,
            Network.Custom(),
            "signed",
        ),
    )

    private val beaconResponses: List<BeaconResponse> = listOf(
        PermissionBeaconResponse(
            messageId,
            "publicKey",
            Network.Custom(),
            emptyList(),
        ),
        OperationBeaconResponse(
            messageId,
            "transactionHash",
        ),
        SignPayloadBeaconResponse(
            messageId,
            "signature",
        ),
        BroadcastBeaconResponse(
            messageId,
            "transactionHash",
        ),
    )

    private inline fun <reified T : BeaconRequest> beaconVersionedRequest(): T = beaconRequests.filterIsInstance<T>().first()
    private inline fun <reified T : BeaconResponse> beaconResponse(): T = beaconResponses.filterIsInstance<T>().first()
}