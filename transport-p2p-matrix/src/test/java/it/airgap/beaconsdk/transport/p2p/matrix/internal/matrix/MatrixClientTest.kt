package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import it.airgap.beaconsdk.core.exception.BeaconException
import it.airgap.beaconsdk.core.internal.utils.Poller
import it.airgap.beaconsdk.core.internal.utils.failure
import it.airgap.beaconsdk.transport.p2p.matrix.data.MatrixRoom
import it.airgap.beaconsdk.transport.p2p.matrix.internal.BeaconP2pMatrixConfiguration
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.MatrixEvent
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.MatrixSync
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.event.MatrixEventResponse
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.login.MatrixLoginResponse
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.room.MatrixCreateRoomRequest
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.room.MatrixCreateRoomResponse
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.room.MatrixInviteRoomResponse
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.room.MatrixJoinRoomResponse
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncResponse
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncRoom
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncRooms
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncState
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncStateEvent.Create
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncStateEvent.Member
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.sync.MatrixSyncStateEvent.Message
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.event.MatrixEventService
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.node.MatrixNodeService
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.room.MatrixRoomService
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.user.MatrixUserService
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.store.HardReset
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.store.Init
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.store.MatrixStore
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.store.MatrixStoreState
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.store.OnSyncError
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.store.OnSyncSuccess
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.store.OnTxnIdCreated
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.store.Reset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import mockLog
import mockTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class MatrixClientTest {

    @MockK(relaxed = true)
    private lateinit var store: MatrixStore

    @MockK
    private lateinit var nodeService: MatrixNodeService

    @MockK
    private lateinit var userService: MatrixUserService

    @MockK
    private lateinit var roomService: MatrixRoomService

    @MockK
    private lateinit var eventService: MatrixEventService

    private lateinit var poller: Poller

    private lateinit var client: MatrixClient

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockLog()

        poller = spyk(Poller())
        client = spyk(MatrixClient(store, nodeService, userService, roomService, eventService, poller))
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `emits sync events`() {
        val events = listOf(
            MatrixEvent.TextMessage("node", "1", "sender#1", "message#1"),
            MatrixEvent.TextMessage("node", "1", "sender#1", "message#2"),
            MatrixEvent.Invite("node", "2", "sender#1"),
        )
        val scope = CoroutineScope(Dispatchers.Default)

        coEvery { store.events } returns flow { events.forEach { emit(it) } }.shareIn(scope, SharingStarted.Lazily)

        runTest {
            val emitted = client.events.take(events.size).toList()

            assertEquals(events.sortedBy { it.toString() }, emitted.sortedBy { it.toString() })
        }
    }

    @Test
    fun `returns joined rooms`() {
        coEvery { store.state() } returns (
            Result.success(
                MatrixStoreState(rooms = mapOf(
                    "1" to MatrixRoom.Invited("1", listOf("member#1")),
                    "2" to MatrixRoom.Joined("2", listOf("member#2")),
                    "3" to MatrixRoom.Joined("3", listOf("member#3")),
                    "4" to MatrixRoom.Left("4", listOf("member#4")),
                ))
            )
        )

        runTest {
            val joined = client.joinedRooms()

            assertEquals(
                listOf(MatrixRoom.Joined("2", listOf("member#2")), MatrixRoom.Joined("3", listOf("member#3"))),
                joined,
            )
        }
    }

    @Test
    fun `returns invited rooms`() {
        coEvery { store.state() } returns (
            Result.success(
                MatrixStoreState(rooms = mapOf(
                    "1" to MatrixRoom.Joined("1", listOf("member#1")),
                    "2" to MatrixRoom.Invited("2", listOf("member#2")),
                    "3" to MatrixRoom.Invited("3", listOf("member#3")),
                    "4" to MatrixRoom.Left("4", listOf("member#4")),
                ))
            )
        )

        runTest {
            val invited = client.invitedRooms()

            assertEquals(
                listOf(MatrixRoom.Invited("2", listOf("member#2")), MatrixRoom.Invited("3", listOf("member#3"))),
                invited,
            )
        }
    }

    @Test
    fun `returns left rooms`() {
        coEvery { store.state() } returns (
            Result.success(
               MatrixStoreState(rooms = mapOf(
                    "1" to MatrixRoom.Joined("1", listOf("member#1")),
                    "2" to MatrixRoom.Invited("2", listOf("member#2")),
                    "3" to MatrixRoom.Left("3", listOf("member#3")),
                    "4" to MatrixRoom.Left("4", listOf("member#4")),
                ))
            )
        )

        runTest {
            val left = client.leftRooms()

            assertEquals(
                listOf(MatrixRoom.Left("3", listOf("member#3")), MatrixRoom.Left("4", listOf("member#4"))),
                left,
            )
        }
    }

    @Test
    fun `returns its login status`() {
        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = null))

        runTest { assertFalse(client.isLoggedIn(), "Expected client not to be logged in") }

        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = "accessToken"))

        runTest { assertTrue(client.isLoggedIn(), "Expected client to be logged in") }
    }

    @Test
    fun `checks if node is up`() {
        val upNode = "upNode"
        val downNode = "downNode"

        coEvery { nodeService.isUp(upNode) } returns true
        coEvery { nodeService.isUp(downNode) } returns false

        runBlocking {
            assertTrue(client.isUp(upNode))
            assertFalse(client.isUp(downNode))
        }
    }

    @Test
    fun `logs in and starts polling for syncs`() {
        val node = "node"
        val userId = "userId"
        val password = "password"
        val deviceId = "deviceId"
        val accessToken = "accessToken"
        val nextSyncToken = "nextSyncToken"

        val testDeferred = CompletableDeferred<Unit>()

        coEvery { userService.login(any(), any(), any(), any()) } returns Result.success(MatrixLoginResponse(userId, deviceId, accessToken))
        coEvery { eventService.sync(any(), any(), any(), any()) } returns Result.success(MatrixSyncResponse(nextBatch = nextSyncToken))

        coEvery { poller.poll<Any>(any(), any(), any()) } answers {
            flow {
                emit(thirdArg<suspend () -> Result<Any>>()())
                testDeferred.complete(Unit)
            }
        }

        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = accessToken))

        runBlocking {
            client.start(node, userId, password, deviceId)

            testDeferred.await()

            coVerify(exactly = 1) { userService.login(node, userId, password, deviceId) }
            coVerify { eventService.sync(node, accessToken) }

            coVerify(exactly = 1) { store.intent(Init(userId, deviceId, accessToken)) }
            coVerify(exactly = 1) { store.intent(OnSyncSuccess(nextSyncToken, pollingTimeout = 30000, null, null)) }

            verify(exactly = 1) { client.syncPollFlow(any(), node) }
            verify(exactly = 1) { poller.poll<MatrixSyncResponse>(any(), 0, any()) }

            confirmVerified(userService, poller)
        }
    }

    @Test
    fun `fails to start if login failed`() {
        val error = IOException()

        coEvery { userService.login(any(), any(), any(), any()) } returns Result.failure(error)

        runTest {
            val exception = assertFailsWith<BeaconException> {
                client.start("node", "userId", "password", "deviceId").getOrThrow()
            }

            assertEquals(error, exception.cause)
        }
    }

    @Test
    fun `fails to start if no access token was returned on login`() {
        coEvery { userService.login(any(), any(), any(), any()) } returns Result.success(MatrixLoginResponse())

        runTest {
            assertFailsWith<BeaconException> {
                client.start("node", "userId", "password", "deviceId").getOrThrow()
            }
        }
    }

    @Test
    fun `stops node`() {
        val node = "node"
        val userId = "userId"
        val password = "password"
        val deviceId = "deviceId"
        val accessToken = "accessToken"

        val testDeferred = CompletableDeferred<Unit>()

        coEvery { userService.login(any(), any(), any(), any()) } returns Result.success(MatrixLoginResponse(userId, deviceId, accessToken))
        coEvery { poller.poll<Any>(any(), any(), any()) } returns (
            flow<Result<Unit>> { while (true) { delay(10000) } }.onCompletion { testDeferred.complete(Unit) }
        )

        runBlocking {
            client.start(node, userId, password, deviceId)
            delay(200)
            client.stop(node)

            testDeferred.await()

            coVerify(exactly = 1) { store.intent(Reset) }
        }
    }

    @Test
    fun `stops only specified node`() {
        val node1 = "node1"
        val node2 = "node2"

        val userId = "userId"
        val password = "password"
        val deviceId = "deviceId"
        val accessToken = "accessToken"

        val testDeferred1 = CompletableDeferred<Unit>()
        val testDeferred2 = CompletableDeferred<Unit>()

        coEvery { userService.login(any(), any(), any(), any()) } returns Result.success(MatrixLoginResponse(userId, deviceId, accessToken))

        coEvery { poller.poll<Any>(any(), any(), any()) } returns (
            flow<Result<Unit>> { while (true) { delay(10000) } }.onCompletion { testDeferred1.complete(Unit) }
        ) andThen (
            flow<Result<Unit>> { while (true) { delay(10000) } }.onCompletion { testDeferred2.complete(Unit) }
        )

        runBlocking {
            client.start(node1, userId, password, deviceId)
            delay(200)
            client.start(node2, userId, password, deviceId)
            delay(200)
            client.stop(node1)

            assertNotNull(withTimeoutOrNull(1000) { testDeferred1.await() })
            assertNull(withTimeoutOrNull(1000) { testDeferred2.await() })
            coVerify(exactly = 0) { store.intent(Reset) }
        }
    }

    @Test
    fun `stops all nodes`() {
        val node1 = "node1"
        val node2 = "node2"

        val userId = "userId"
        val password = "password"
        val deviceId = "deviceId"
        val accessToken = "accessToken"

        val testDeferred1 = CompletableDeferred<Unit>()
        val testDeferred2 = CompletableDeferred<Unit>()

        coEvery { userService.login(any(), any(), any(), any()) } returns Result.success(MatrixLoginResponse(userId, deviceId, accessToken))

        coEvery { poller.poll<Any>(any(), any(), any()) } returns (
            flow<Result<Unit>> { while (true) { delay(10000) } }.onCompletion { testDeferred1.complete(Unit) }
        ) andThen (
            flow<Result<Unit>> { while (true) { delay(10000) } }.onCompletion { testDeferred2.complete(Unit) }
        )

        runBlocking {
            client.start(node1, userId, password, deviceId)
            client.start(node2, userId, password, deviceId)
            delay(200)
            client.stop()

            testDeferred1.await()
            testDeferred2.await()

            coVerify(exactly = 1) { store.intent(Reset) }
        }
    }

    @Test
    fun `resets connection`() {
        val node = "node"
        runTest {
            client.resetHard(node)

            coVerify(exactly = 1) { client.stop(node) }
            coVerify(exactly = 1) { store.intent(HardReset) }
        }
    }

    @Test
    fun `creates trusted private room`() {
        val node = "node"
        val accessToken = "accessToken"
        val members = listOf("member#1", "member#2")
        val roomId = "roomId"

        coEvery { roomService.createRoom(any(), any(), any()) } returns Result.success(MatrixCreateRoomResponse(roomId))
        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = accessToken))

        runTest {
            val room = client.createTrustedPrivateRoom(node, *members.toTypedArray()).getOrThrow()

            assertEquals(MatrixRoom.Unknown(roomId), room)
            coVerify { roomService.createRoom(
                node,
                accessToken,
                MatrixCreateRoomRequest(invite = members, preset = MatrixCreateRoomRequest.Preset.TrustedPrivateChat, isDirect = true, roomVersion = "5"),
            ) }

            confirmVerified(roomService)
        }
    }

    @Test
    fun `fails to create trusted private room if has no access token`() {
        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = null))

        runTest {
            val result = client.createTrustedPrivateRoom("node")

            assertTrue(result.isFailure, "Expected room create result to be a failure")
        }
    }

    @Test
    fun `invites user to room specified by its id`() {
        val node = "node"
        val accessToken = "accessToken"
        val user = "user"
        val roomId = "1"

        coEvery { roomService.inviteToRoom(any(), any(), any(), any()) } returns Result.success(
            MatrixInviteRoomResponse())
        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = accessToken))

        runTest {
            val result = client.inviteToRoom(node, user, roomId)

            assertTrue(result.isSuccess, "Expected result to be a success")
            coVerify(exactly = 1) { roomService.inviteToRoom(node, accessToken, user, roomId) }

            confirmVerified(roomService)
        }
    }

    @Test
    fun `invites user to specified rooms`() {
        val node = "node"
        val accessToken = "accessToken"
        val user = "user"
        val room = MatrixRoom.Joined("1", emptyList())

        coEvery { roomService.inviteToRoom(any(), any(), any(), any()) } returns Result.success(MatrixInviteRoomResponse())
        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = accessToken))

        runTest {
            val result = client.inviteToRoom(node, user, room)

            assertTrue(result.isSuccess, "Expected result to be a success")
            coVerify(exactly = 1) {
                roomService.inviteToRoom(
                    node,
                    accessToken,
                    user,
                    room.id,
                )
            }

            confirmVerified(roomService)
        }
    }

    @Test
    fun `fails to invite to rooms specified by ids if has no access token`() {
        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = null))

        runTest {
            val result = client.inviteToRoom("node", "user", "1")

            assertTrue(result.isFailure, "Expected room create result to be a failure")
        }
    }

    @Test
    fun `fails to invite to rooms if has no access token`() {
        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = null))

        runTest {
            val result = client.inviteToRoom("node", "user", MatrixRoom.Joined("1", emptyList()))

            assertTrue(result.isFailure, "Expected room create result to be a failure")
        }
    }

    @Test
    fun `joins rooms specified by their ids`() {
        val node = "node"
        val accessToken = "accessToken"
        val user = "user"
        val roomId = "1"

        coEvery { roomService.joinRoom(any(), any(), any()) } returns Result.success(
            MatrixJoinRoomResponse(user))
        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = accessToken))

        runTest {
            val result = client.joinRoom(node, roomId)

            assertTrue(result.isSuccess, "Expected result to be a success")
            coVerify(exactly = 1) { roomService.joinRoom(node, accessToken, roomId) }

            confirmVerified(roomService)
        }
    }

    @Test
    fun `joins specified rooms`() {
        val node = "node"
        val accessToken = "accessToken"
        val user = "user"
        val room = MatrixRoom.Joined("1", emptyList())

        coEvery { roomService.joinRoom(any(), any(), any()) } returns Result.success(MatrixJoinRoomResponse(user))
        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = accessToken))

        runTest {
            val result = client.joinRoom(node, room)

            assertTrue(result.isSuccess, "Expected result to be a success")
            coVerify(exactly = 1) {
                roomService.joinRoom(
                    node,
                    accessToken,
                    room.id,
                )
            }

            confirmVerified(roomService)
        }
    }

    @Test
    fun `fails to join rooms specified by ids if has no access token`() {
        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = null))

        runTest {
            val result = client.joinRoom("node", "1")

            assertTrue(result.isFailure, "Expected room create result to be a failure")
        }
    }

    @Test
    fun `fails to join rooms if has no access token`() {
        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = null))

        runTest {
            val result = client.joinRoom("node", MatrixRoom.Joined("1", emptyList()))

            assertTrue(result.isFailure, "Expected room create result to be a failure")
        }
    }

    @Test
    fun `sends message to room specified by its id`() {
        val node = "node"
        val accessToken = "accessToken"
        val roomId = "1"
        val message = "message"
        val currentTime = 1L

        mockTime(currentTime)

        coEvery { eventService.sendTextMessage(any(), any(), any(), any(), any()) } returns Result.success(
            MatrixEventResponse())
        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = accessToken, transactionCounter = 1))

        runTest {
            val result = client.sendTextMessage(node, roomId, message)

            assertTrue(result.isSuccess, "Expected result to be a success")
            coVerify {
                eventService.sendTextMessage(
                    node,
                    accessToken,
                    roomId,
                    "m$currentTime.1",
                    message,
                )
            }
            coVerify { store.intent(OnTxnIdCreated) }

            confirmVerified(eventService)
        }
    }

    @Test
    fun `sends message to specified room`() {
        val node = "node"
        val accessToken = "accessToken"
        val room = MatrixRoom.Joined("1", emptyList())
        val message = "message"
        val currentTime = 1L

        mockTime(currentTime)

        coEvery { eventService.sendTextMessage(any(), any(), any(), any(), any()) } returns Result.success(MatrixEventResponse())
        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = accessToken, transactionCounter = 1))

        runTest {
            val result = client.sendTextMessage(node, room, message)

            assertTrue(result.isSuccess, "Expected result to be a success")
            coVerify {
                eventService.sendTextMessage(
                    node,
                    accessToken,
                    room.id,
                    "m$currentTime.1",
                    message,
                )
            }
            coVerify { store.intent(OnTxnIdCreated) }

            confirmVerified(eventService)
        }
    }

    @Test
    fun `fails to send message to room specified by id if has no access token`() {
        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = null))

        runTest {
            val result = client.inviteToRoom("node", "1", "message")

            assertTrue(result.isFailure, "Expected room create result to be a failure")
        }
    }

    @Test
    fun `fails to send message to room if has no access token`() {
        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = null))

        runTest {
            val result = client.sendTextMessage("node", MatrixRoom.Joined("1", emptyList()), "message")

            assertTrue(result.isFailure, "Expected room create result to be a failure")
        }
    }

    @Test
    fun `syncs for latest events`() {
        val node = "node"
        val accessToken = "accessToken"
        val since = "since"
        val timeout = 1L

        coEvery { eventService.sync(any(), any(), any(), any()) } returns Result.success(MatrixSyncResponse())
        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = accessToken, syncToken = since, pollingTimeout = timeout))

        runTest {
            val response = client.sync(node).getOrThrow()

            assertEquals(MatrixSync(), response)
            coVerify { eventService.sync(node, accessToken, since, timeout) }

            confirmVerified(eventService)
        }
    }

    @Test
    fun `fails to sync if has no access token`() {
        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = null))

        runTest {
            val result = client.sync("node")

            assertTrue(result.isFailure, "Expected room create result to be a failure")
        }
    }

    @Test
    fun `updates store on successful sync poll`() {
        val node = "node"
        val accessToken = "accessToken"
        val nextBatch = "nextBatch"
        val syncRooms = MatrixSyncRooms(
            join = mapOf(
                "1" to MatrixSyncRoom.Joined(MatrixSyncState(listOf(
                    Message(content = Message.Content(Message.Content.TYPE_TEXT, body = "message"), sender = "sender#1"),
                    Member(content = Member.Content(Member.Membership.Join), sender = "sender#2"),
                    Create(content = Create.Content("sender#3"), sender = "sender#3"),
                )))
            )
        )

        coEvery { eventService.sync(any(), any(), any()) } returns Result.success(MatrixSyncResponse(nextBatch, syncRooms))
        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = accessToken))

        runBlocking {
            client.syncPollFlow(this, node).take(1).collect()

            coVerify {
                store.intent(
                    OnSyncSuccess(
                        syncToken = nextBatch,
                        pollingTimeout = 30000,
                        rooms = MatrixRoom.fromSync(node, syncRooms),
                        events = MatrixEvent.fromSync(node, syncRooms)
                    )
                )
            }
        }
    }

    @Test
    fun `updates retry counter on error and continues polling if max not exceeded`() {
        val node = "node"
        val accessToken = "accessToken"

        coEvery { eventService.sync(any(), any(), any()) } returns Result.failure()
        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = accessToken))

        runTest {
            val scope = CoroutineScope(Dispatchers.Default)
            client.syncPollFlow(scope, node).take(1).collect()

            assertTrue(scope.isActive, "Expected scope to be active")
            coVerify { store.intent(OnSyncError) }
        }
    }

    @Test
    fun `cancels polling on max retries exceeded`() {
        val node = "node"
        val accessToken = "accessToken"

        coEvery { eventService.sync(any(), any(), any()) } returns Result.failure()
        coEvery { store.state() } returns Result.success(MatrixStoreState(accessToken = accessToken, pollingRetries = BeaconP2pMatrixConfiguration.MATRIX_MAX_SYNC_RETRIES))

        runTest {
            val scope = CoroutineScope(Dispatchers.Default)
            client.syncPollFlow(scope, node).take(1).collect()

            assertFalse(scope.isActive, "Expected scope to be canceled")
            coVerify { store.intent(OnSyncError) }
        }
    }
}