package it.airgap.beaconsdk.internal.transport.p2p.matrix

import it.airgap.beaconsdk.internal.BeaconConfiguration
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixEvent
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixRoom
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixSync
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.room.MatrixCreateRoomRequest
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.room.MatrixCreateRoomRequest.Preset
import it.airgap.beaconsdk.internal.transport.p2p.matrix.network.MatrixEventService
import it.airgap.beaconsdk.internal.transport.p2p.matrix.network.MatrixRoomService
import it.airgap.beaconsdk.internal.transport.p2p.matrix.network.MatrixUserService
import it.airgap.beaconsdk.internal.transport.p2p.matrix.store.*
import it.airgap.beaconsdk.internal.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex

internal class MatrixClient(
    private val store: MatrixStore,
    private val userService: MatrixUserService,
    private val roomService: MatrixRoomService,
    private val eventService: MatrixEventService,
    private val poller: Poller,
) {
    val events: Flow<MatrixEvent>
        get() = store.events

    private var syncScope: CoroutineScope? = null

    suspend fun joinedRooms(): List<MatrixRoom.Joined> =
        store.state().rooms.values.filterIsInstance<MatrixRoom.Joined>()

    suspend fun invitedRooms(): List<MatrixRoom.Invited> =
        store.state().rooms.values.filterIsInstance<MatrixRoom.Invited>()

    suspend fun leftRooms(): List<MatrixRoom.Left> =
        store.state().rooms.values.filterIsInstance<MatrixRoom.Left>()

    suspend fun isLoggedIn(): Boolean = store.state().accessToken != null

    suspend fun start(userId: String, password: String, deviceId: String) {
        val loginResponse = userService.login(userId, password, deviceId)

        val accessToken = loginResponse.getOrNull()?.accessToken
            ?: failWith("Login failed", loginResponse.errorOrNull())

        store.intent(Init(userId, deviceId, accessToken))
        syncScope { syncPoll(it).collect() }
    }

    suspend fun createTrustedPrivateRoom(vararg members: String): InternalResult<MatrixRoom?> =
        flatTryResult {
            withAccessToken("createTrustedPrivateRoom") { accessToken ->
                roomService.createRoom(
                    accessToken,
                    MatrixCreateRoomRequest(
                        invite = members.toList(),
                        preset = Preset.TrustedPrivateChat,
                        isDirect = true,
                    ),
                ).map { response -> response.roomId?.let { MatrixRoom.Unknown(it) } }
            }
        }

    suspend fun inviteToRooms(user: String, vararg roomIds: String): InternalResult<Unit> =
        tryResult {
            withAccessToken("inviteToRooms") { accessToken ->
                roomIds.toList().launch {
                    roomService.inviteToRoom(accessToken, user, it).get()
                }
            }
        }

    suspend fun inviteToRooms(user: String, vararg rooms: MatrixRoom): InternalResult<Unit> =
        tryResult {
            withAccessToken("inviteToRooms") { accessToken ->
                rooms.toList().launch {
                    roomService.inviteToRoom(accessToken, user, it.id).get()
                }
            }
        }

    suspend fun joinRooms(vararg roomIds: String) =
        tryResult {
            withAccessToken("joinRooms") { accessToken ->
                roomIds.toList().launch {
                    roomService.joinRoom(accessToken, it).get()
                }
            }
        }

    suspend fun joinRooms(vararg rooms: MatrixRoom) =
        tryResult {
            withAccessToken("joinRooms") { accessToken ->
                rooms.toList().launch {
                    roomService.joinRoom(accessToken, it.id).get()
                }
            }
        }

    suspend fun sendTextMessage(roomId: String, message: String) =
        tryResult {
            withAccessToken("sendTextMessage") { accessToken ->
                eventService.sendTextMessage(
                    accessToken,
                    roomId,
                    createTxnId(),
                    message,
                ).get()
            }
        }

    suspend fun sendTextMessage(room: MatrixRoom, message: String) =
        tryResult {
            withAccessToken("sendTextMessage") { accessToken ->
                eventService.sendTextMessage(
                    accessToken,
                    room.id,
                    createTxnId(),
                    message,
                ).get()
            }
        }

    suspend fun sync(): InternalResult<MatrixSync> =
        flatTryResult {
            withAccessToken("sync") { accessToken ->
                eventService.sync(accessToken, store.state().syncToken, store.state().pollingTimeout)
                    .map { MatrixSync.fromSyncResponse(it) }
            }
        }

    fun syncPoll(scope: CoroutineScope, interval: Long = 0): Flow<InternalResult<MatrixSync>> {
        val syncMutex = Mutex()

        return poller.poll(interval = interval) {
            syncMutex.lock()
            sync()
        }.onEach {
            when (it) {
                is Success -> onSyncSuccess(it.value)
                is Failure -> onSyncError(scope, it.error)
            }
            syncMutex.unlock()
        }
    }

    private suspend inline fun <T> withAccessToken(
        name: String,
        block: (accessToken: String) -> T,
    ): T {
        val accessToken = store.state().accessToken ?: failWithRequiresAuthorization(name)
        return block(accessToken)
    }

    private fun syncScope(block: suspend (CoroutineScope) -> Unit) {
        val scope = syncScope
            ?: CoroutineScope(CoroutineName("sync") + Dispatchers.IO).also { syncScope = it }

        scope.launch {
            block(scope)
            syncScope = null
        }
    }

    private suspend fun onSyncSuccess(sync: MatrixSync) {
        store.intent(
            OnSyncSuccess(
                syncToken = sync.nextBatch,
                pollingTimeout = 30000,
                rooms = sync.rooms,
                events = sync.events,
            )
        )
    }

    private suspend fun onSyncError(scope: CoroutineScope, error: Throwable?) {
        store.intent(OnSyncError)

        error?.let { logError(TAG, it) }
        if (store.state().pollingRetries >= BeaconConfiguration.MATRIX_MAX_SYNC_RETRIES) {
            logDebug(TAG, "Max sync retries exceeded")
            scope.cancel()
        } else {
            logDebug(TAG, "Retry syncing...")
        }
    }

    private suspend fun createTxnId(): String {
        val timestamp = currentTimestamp()
        val counter = store.state().transactionCounter

        store.intent(OnTxnIdCreated)

        return "m$timestamp.$counter"
    }

    private fun failWithRequiresAuthorization(name: String): Nothing =
        throw IllegalStateException("Authorization required but no access token has been provided ($name)")

    companion object {
        const val TAG = "MatrixClient"
    }
}