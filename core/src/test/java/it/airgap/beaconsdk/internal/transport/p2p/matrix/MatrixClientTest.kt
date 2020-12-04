package it.airgap.beaconsdk.internal.transport.p2p.matrix

import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.exception.BeaconException
import it.airgap.beaconsdk.internal.BeaconConfiguration
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixEvent
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixRoom
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixSync
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.event.MatrixEventResponse
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.login.MatrixLoginResponse
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.room.MatrixCreateRoomRequest
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.room.MatrixCreateRoomRequest.Preset
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.room.MatrixCreateRoomResponse
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.room.MatrixInviteRoomResponse
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.room.MatrixJoinRoomResponse
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncResponse
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncRoom
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncRooms
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncState
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncStateEvent.*
import it.airgap.beaconsdk.internal.transport.p2p.matrix.network.MatrixEventService
import it.airgap.beaconsdk.internal.transport.p2p.matrix.network.MatrixRoomService
import it.airgap.beaconsdk.internal.transport.p2p.matrix.network.MatrixUserService
import it.airgap.beaconsdk.internal.transport.p2p.matrix.store.*
import it.airgap.beaconsdk.internal.utils.Failure
import it.airgap.beaconsdk.internal.utils.Poller
import it.airgap.beaconsdk.internal.utils.Success
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import mockLog
import mockTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class MatrixClientTest {

    @MockK(relaxed = true)
    private lateinit var store: MatrixStore

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
        client = spyk(MatrixClient(store, userService, roomService, eventService, poller))
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `emits sync events`() {
        val events = listOf(
            MatrixEvent.TextMessage("1", "sender#1", "message#1"),
            MatrixEvent.TextMessage("1", "sender#1", "message#2"),
            MatrixEvent.Invite("2"),
        )
        coEvery { store.events } returns flow { events.forEach { emit(it) } }.shareIn(TestCoroutineScope(), SharingStarted.Lazily)

        runBlockingTest {
            val emitted = client.events.take(events.size).toList()

            assertEquals(events.sortedBy { it.toString() }, emitted.sortedBy { it.toString() })
        }
    }

    @Test
    fun `returns joined rooms`() {
        coEvery { store.state() } returns MatrixStoreState(rooms = mapOf(
            "1" to MatrixRoom.Invited("1", listOf("member#1")),
            "2" to MatrixRoom.Joined("2", listOf("member#2")),
            "3" to MatrixRoom.Joined("3", listOf("member#3")),
            "4" to MatrixRoom.Left("4", listOf("member#4")),
        ))

        runBlockingTest {
            val joined = client.joinedRooms()

            assertEquals(
                listOf(MatrixRoom.Joined("2", listOf("member#2")), MatrixRoom.Joined("3", listOf("member#3"))),
                joined,
            )
        }
    }

    @Test
    fun `returns invited rooms`() {
        coEvery { store.state() } returns MatrixStoreState(rooms = mapOf(
            "1" to MatrixRoom.Joined("1", listOf("member#1")),
            "2" to MatrixRoom.Invited("2", listOf("member#2")),
            "3" to MatrixRoom.Invited("3", listOf("member#3")),
            "4" to MatrixRoom.Left("4", listOf("member#4")),
        ))

        runBlockingTest {
            val invited = client.invitedRooms()

            assertEquals(
                listOf(MatrixRoom.Invited("2", listOf("member#2")), MatrixRoom.Invited("3", listOf("member#3"))),
                invited,
            )
        }
    }

    @Test
    fun `returns left rooms`() {
        coEvery { store.state() } returns MatrixStoreState(rooms = mapOf(
            "1" to MatrixRoom.Joined("1", listOf("member#1")),
            "2" to MatrixRoom.Invited("2", listOf("member#2")),
            "3" to MatrixRoom.Left("3", listOf("member#3")),
            "4" to MatrixRoom.Left("4", listOf("member#4")),
        ))

        runBlockingTest {
            val left = client.leftRooms()

            assertEquals(
                listOf(MatrixRoom.Left("3", listOf("member#3")), MatrixRoom.Left("4", listOf("member#4"))),
                left,
            )
        }
    }

    @Test
    fun `returns its login status`() {
        coEvery { store.state() } returns MatrixStoreState(accessToken = null)

        runBlockingTest { assertFalse(client.isLoggedIn(), "Expected client not to be logged in") }

        coEvery { store.state() } returns MatrixStoreState(accessToken = "accessToken")

        runBlockingTest { assertTrue(client.isLoggedIn(), "Expected client to be logged in") }
    }

    @Test
    fun `logs in and starts polling for syncs`() {
        val userId = "userId"
        val password = "password"
        val deviceId = "deviceId"
        val accessToken = "accessToken"

        coEvery { userService.login(any(), any(), any()) } returns Success(MatrixLoginResponse(userId, deviceId, accessToken))
        coEvery { poller.poll<Any>(any(), any(), any()) } returns flow {  }

        runBlockingTest {
            client.start(userId, password, deviceId)

            coVerify { userService.login(userId, password, deviceId) }
            coVerify { store.intent(Init(userId, deviceId, accessToken)) }
            verify { client.syncPoll(any()) }
            verify { poller.poll<MatrixSyncResponse>(any(), 0, any()) }

            confirmVerified(userService, store, poller)
        }
    }

    @Test
    fun `fails to start if login failed`() {
        val error = IOException()

        coEvery { userService.login(any(), any(), any()) } returns Failure(error)

        runBlockingTest {
            val exception = assertFailsWith<BeaconException> {
                client.start("userId", "password", "deviceId")
            }

            assertEquals(error, exception.cause)
        }
    }

    @Test
    fun `fails to start if no access token was returned on login`() {
        coEvery { userService.login(any(), any(), any()) } returns Success(MatrixLoginResponse())

        runBlockingTest {
            assertFailsWith<BeaconException> {
                client.start("userId", "password", "deviceId")
            }
        }
    }

    @Test
    fun `creates trusted private room`() {
        val accessToken = "accessToken"
        val members = listOf("member#1", "member#2")
        val roomId = "roomId"

        coEvery { roomService.createRoom(any(), any()) } returns Success(MatrixCreateRoomResponse(roomId))
        coEvery { store.state() } returns MatrixStoreState(accessToken = accessToken)

        runBlockingTest {
            val room = client.createTrustedPrivateRoom(*members.toTypedArray()).get()

            assertEquals(MatrixRoom.Unknown(roomId), room)
            coVerify { roomService.createRoom(
                accessToken,
                MatrixCreateRoomRequest(invite = members, preset = Preset.TrustedPrivateChat, isDirect = true),
            ) }

            confirmVerified(roomService)
        }
    }

    @Test
    fun `fails to create trusted private room if has no access token`() {
        coEvery { store.state() } returns MatrixStoreState(accessToken = null)

        runBlockingTest {
            val result = client.createTrustedPrivateRoom()

            assertTrue(result.isFailure, "Expected room create result to be a failure")
        }
    }

    @Test
    fun `invites user to rooms specified by their ids`() {
        val accessToken = "accessToken"
        val user = "user"
        val roomIds = listOf("1", "2", "3")

        coEvery { roomService.inviteToRoom(any(), any(), any()) } returns Success(MatrixInviteRoomResponse())
        coEvery { store.state() } returns MatrixStoreState(accessToken = accessToken)

        runBlockingTest {
            val result = client.inviteToRooms(user, *roomIds.toTypedArray())

            assertTrue(result.isSuccess, "Expected result to be a success")
            coVerify(exactly = roomIds.size) { roomService.inviteToRoom(accessToken, user, match { roomIds.contains(it) }) }

            confirmVerified(roomService)
        }
    }

    @Test
    fun `invites user to specified rooms`() {
        val accessToken = "accessToken"
        val user = "user"
        val rooms = listOf(
            MatrixRoom.Joined("1", emptyList()),
            MatrixRoom.Joined("2", emptyList()),
        )

        coEvery { roomService.inviteToRoom(any(), any(), any()) } returns Success(MatrixInviteRoomResponse())
        coEvery { store.state() } returns MatrixStoreState(accessToken = accessToken)

        runBlockingTest {
            val result = client.inviteToRooms(user, *rooms.toTypedArray())

            assertTrue(result.isSuccess, "Expected result to be a success")
            coVerify(exactly = rooms.size) {
                roomService.inviteToRoom(
                    accessToken,
                    user,
                    match { rooms.map(MatrixRoom::id).contains(it) }
                )
            }

            confirmVerified(roomService)
        }
    }

    @Test
    fun `fails to invite to rooms specified by ids if has no access token`() {
        coEvery { store.state() } returns MatrixStoreState(accessToken = null)

        runBlockingTest {
            val result = client.inviteToRooms("user", "1")

            assertTrue(result.isFailure, "Expected room create result to be a failure")
        }
    }

    @Test
    fun `fails to invite to rooms if has no access token`() {
        coEvery { store.state() } returns MatrixStoreState(accessToken = null)

        runBlockingTest {
            val result = client.inviteToRooms("user", MatrixRoom.Joined("1", emptyList()))

            assertTrue(result.isFailure, "Expected room create result to be a failure")
        }
    }

    @Test
    fun `joins rooms specified by their ids`() {
        val accessToken = "accessToken"
        val user = "user"
        val roomIds = listOf("1", "2", "3")

        coEvery { roomService.joinRoom(any(), any()) } returns Success(MatrixJoinRoomResponse(user))
        coEvery { store.state() } returns MatrixStoreState(accessToken = accessToken)

        runBlockingTest {
            val result = client.joinRooms(*roomIds.toTypedArray())

            assertTrue(result.isSuccess, "Expected result to be a success")
            coVerify(exactly = roomIds.size) { roomService.joinRoom(accessToken, match { roomIds.contains(it) }) }

            confirmVerified(roomService)
        }
    }

    @Test
    fun `joins specified rooms`() {
        val accessToken = "accessToken"
        val user = "user"
        val rooms = listOf(
            MatrixRoom.Joined("1", emptyList()),
            MatrixRoom.Joined("2", emptyList()),
        )

        coEvery { roomService.joinRoom(any(), any()) } returns Success(MatrixJoinRoomResponse(user))
        coEvery { store.state() } returns MatrixStoreState(accessToken = accessToken)

        runBlockingTest {
            val result = client.joinRooms(*rooms.toTypedArray())

            assertTrue(result.isSuccess, "Expected result to be a success")
            coVerify(exactly = rooms.size) {
                roomService.joinRoom(
                    accessToken,
                    match { rooms.map(MatrixRoom::id).contains(it) }
                )
            }

            confirmVerified(roomService)
        }
    }

    @Test
    fun `fails to join rooms specified by ids if has no access token`() {
        coEvery { store.state() } returns MatrixStoreState(accessToken = null)

        runBlockingTest {
            val result = client.joinRooms("1")

            assertTrue(result.isFailure, "Expected room create result to be a failure")
        }
    }

    @Test
    fun `fails to join rooms if has no access token`() {
        coEvery { store.state() } returns MatrixStoreState(accessToken = null)

        runBlockingTest {
            val result = client.joinRooms(MatrixRoom.Joined("1", emptyList()))

            assertTrue(result.isFailure, "Expected room create result to be a failure")
        }
    }

    @Test
    fun `sends message to room specified by its id`() {
        val accessToken = "accessToken"
        val roomId = "1"
        val message = "message"
        val currentTime = 1L

        mockTime(currentTime)

        coEvery { eventService.sendTextMessage(any(), any(), any(), any()) } returns Success(MatrixEventResponse())
        coEvery { store.state() } returns MatrixStoreState(accessToken = accessToken, transactionCounter = 1)

        runBlockingTest {
            val result = client.sendTextMessage(roomId, message)

            assertTrue(result.isSuccess, "Expected result to be a success")
            coVerify {
                eventService.sendTextMessage(
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
        val accessToken = "accessToken"
        val room = MatrixRoom.Joined("1", emptyList())
        val message = "message"
        val currentTime = 1L

        mockTime(currentTime)

        coEvery { eventService.sendTextMessage(any(), any(), any(), any()) } returns Success(MatrixEventResponse())
        coEvery { store.state() } returns MatrixStoreState(accessToken = accessToken, transactionCounter = 1)

        runBlockingTest {
            val result = client.sendTextMessage(room, message)

            assertTrue(result.isSuccess, "Expected result to be a success")
            coVerify {
                eventService.sendTextMessage(
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
        coEvery { store.state() } returns MatrixStoreState(accessToken = null)

        runBlockingTest {
            val result = client.inviteToRooms("1", "message")

            assertTrue(result.isFailure, "Expected room create result to be a failure")
        }
    }

    @Test
    fun `fails to send message to room if has no access token`() {
        coEvery { store.state() } returns MatrixStoreState(accessToken = null)

        runBlockingTest {
            val result = client.sendTextMessage(MatrixRoom.Joined("1", emptyList()), "message")

            assertTrue(result.isFailure, "Expected room create result to be a failure")
        }
    }

    @Test
    fun `syncs for latest events`() {
        val accessToken = "accessToken"
        val since = "since"
        val timeout = 1L

        coEvery { eventService.sync(any(), any(), any()) } returns Success(MatrixSyncResponse())
        coEvery { store.state() } returns MatrixStoreState(accessToken = accessToken, syncToken = since, pollingTimeout = timeout)

        runBlockingTest {
            val response = client.sync().get()

            assertEquals(MatrixSync(), response)
            coVerify { eventService.sync(accessToken, since, timeout) }

            confirmVerified(eventService)
        }
    }

    @Test
    fun `fails to sync if has no access token`() {
        coEvery { store.state() } returns MatrixStoreState(accessToken = null)

        runBlockingTest {
            val result = client.sync()

            assertTrue(result.isFailure, "Expected room create result to be a failure")
        }
    }

    @Test
    fun `updates store on successful sync poll`() {
        val accessToken = "accessToken"

        val nextBatch = "nextBatch"
        val syncRooms = MatrixSyncRooms(
            join = mapOf(
                "1" to MatrixSyncRoom.Joined(MatrixSyncState(listOf(
                    Message(content = Message.Content(Message.TYPE_TEXT, body = "message"), sender = "sender#1"),
                    Member(content = Member.Content(Member.Membership.Join), sender = "sender#2"),
                    Create(content = Create.Content("sender#3"), sender = "sender#3"),
                )))
            )
        )

        coEvery { eventService.sync(any(), any(), any()) } returns Success(MatrixSyncResponse(nextBatch, syncRooms))
        coEvery { store.state() } returns MatrixStoreState(accessToken = accessToken)

        runBlocking {
            client.syncPoll(CoroutineScope(TestCoroutineDispatcher())).take(1).collect()

            coVerify {
                store.intent(
                    OnSyncSuccess(
                        syncToken = nextBatch,
                        pollingTimeout = 30000,
                        rooms = MatrixRoom.fromSync(syncRooms),
                        events = MatrixEvent.fromSync(syncRooms).filterNotNull()
                    )
                )
            }
        }
    }

    @Test
    fun `updates retry counter on error and continues polling if max not exceeded`() {
        val accessToken = "accessToken"

        coEvery { eventService.sync(any(), any(), any()) } returns Failure()
        coEvery { store.state() } returns MatrixStoreState(accessToken = accessToken)

        runBlocking {
            val scope = CoroutineScope(TestCoroutineDispatcher())
            client.syncPoll(scope).take(1).collect()

            assertTrue(scope.isActive, "Expected scope to be active")
            coVerify { store.intent(OnSyncError) }
        }
    }

    @Test
    fun `cancels polling on max retries exceeded`() {
        val accessToken = "accessToken"

        coEvery { eventService.sync(any(), any(), any()) } returns Failure()
        coEvery { store.state() } returns MatrixStoreState(accessToken = accessToken, pollingRetries = BeaconConfiguration.matrixMaxSyncRetries)

        runBlocking {
            val scope = CoroutineScope(TestCoroutineDispatcher())
            client.syncPoll(scope).take(1).collect()

            assertFalse(scope.isActive, "Expected scope to be canceled")
            coVerify { store.intent(OnSyncError) }
        }
    }
}