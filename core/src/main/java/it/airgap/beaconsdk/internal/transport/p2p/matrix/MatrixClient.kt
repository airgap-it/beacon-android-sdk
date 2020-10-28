package it.airgap.beaconsdk.internal.transport.p2p.matrix

import it.airgap.beaconsdk.internal.BeaconConfig
import it.airgap.beaconsdk.internal.network.HttpPoller
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.event.MatrixStateEvent
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.room.MatrixCreateRoomRequest
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncResponse
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.client.MatrixEvent
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.client.MatrixRoom
import it.airgap.beaconsdk.internal.transport.p2p.matrix.network.MatrixEventService
import it.airgap.beaconsdk.internal.transport.p2p.matrix.network.MatrixRoomService
import it.airgap.beaconsdk.internal.transport.p2p.matrix.network.MatrixUserService
import it.airgap.beaconsdk.internal.transport.p2p.matrix.store.MatrixStore
import it.airgap.beaconsdk.internal.transport.p2p.matrix.store.MatrixStoreAction.*
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
    private val httpPoller: HttpPoller,
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
            ?: failWith(ERROR_MESSAGE_LOGIN_FAILED, loginResponse.getErrorOrNull())

        store.intent(Init(userId, deviceId, accessToken))
        syncPoll()
    }

    suspend fun createTrustedPrivateRoom(vararg members: String): InternalResult<MatrixRoom?> =
        flatTryResult {
            withAccessToken("createTrustedPrivateRoom") { accessToken ->
                roomService.createRoom(
                    accessToken,
                    MatrixCreateRoomRequest(
                        invite = members.toList(),
                        preset = MatrixCreateRoomRequest.Preset.TrustedPrivateChat,
                        isDirect = true,
                    ),
                ).map { response -> response.roomId?.let { MatrixRoom.Unknown(it) } }
            }
        }

    suspend fun inviteToRooms(user: String, vararg roomIds: String): InternalResult<Unit> =
        tryResult {
            withAccessToken("inviteToRooms") { accessToken ->
                roomIds.toList().launch { it ->
                    roomService.inviteToRoom(accessToken, user, it).getOrThrow()
                }
            }
        }

    suspend fun inviteToRooms(user: String, vararg rooms: MatrixRoom): InternalResult<Unit> =
        tryResult {
            withAccessToken("inviteToRooms") { accessToken ->
                rooms.toList().launch { it ->
                    roomService.inviteToRoom(accessToken, user, it.id).getOrThrow()
                }
            }
        }

    suspend fun joinRooms(vararg roomIds: String) =
        tryResult {
            withAccessToken("joinRooms") { accessToken ->
                roomIds.toList().launch {
                    roomService.joinRoom(accessToken, it).getOrThrow()
                }
            }
        }

    suspend fun joinRooms(vararg rooms: MatrixRoom) =
        tryResult {
            withAccessToken("joinRooms") { accessToken ->
                rooms.toList().launch {
                    roomService.joinRoom(accessToken, it.id).getOrThrow()
                }
            }
        }

    suspend fun sendTextMessage(roomId: String, message: String) =
        tryResult {
            withAccessToken("sendTextMessage") { accessToken ->
                eventService.sendEvent(
                    accessToken,
                    roomId,
                    MatrixStateEvent.TYPE_MESSAGE,
                    createTxnId(),
                    MatrixStateEvent.Message.Content(
                        MatrixStateEvent.Message.TYPE_TEXT,
                        message
                    )
                ).getOrThrow()
            }
        }

    suspend fun sendTextMessage(room: MatrixRoom, message: String) =
        tryResult {
            withAccessToken("sendTextMessage") { accessToken ->
                eventService.sendEvent(
                    accessToken,
                    room.id,
                    MatrixStateEvent.TYPE_MESSAGE,
                    createTxnId(),
                    MatrixStateEvent.Message.Content(
                        MatrixStateEvent.Message.TYPE_TEXT,
                        message
                    )
                ).getOrThrow()
            }
        }

    private suspend fun syncPoll(interval: Long = 0) {
        syncScope { scope ->
            val syncMutex = Mutex()

            httpPoller
                .poll(interval = interval) {
                    syncMutex.lock()
                    sync()
                }
                .collect {
                    when (it) {
                        is InternalResult.Success -> onSyncSuccess(it.value)
                        is InternalResult.Error -> onSyncError(scope, it.error)
                    }
                    syncMutex.unlock()
                }
        }
    }

    private suspend fun sync(): InternalResult<MatrixSyncResponse> =
        withAccessToken("sync") {
            eventService.sync(it, store.state().syncToken, store.state().pollingTimeout)
        }

    private suspend inline fun <T> withAccessToken(name: String, block: (accessToken: String) -> T): T {
        val accessToken = store.state().accessToken ?: failWithRequiresAuthorization(name)
        return block(accessToken)
    }

    private fun syncScope(block: suspend (CoroutineScope) -> Unit) {
        val scope = syncScope ?: CoroutineScope(CoroutineName("sync") + Dispatchers.IO).also { syncScope = it }
        scope.launch {
            block(scope)
            syncScope = null
        }
    }

    private suspend fun onSyncSuccess(response: MatrixSyncResponse) {
        store.intent(OnSyncSuccess(
            syncToken = response.nextBatch,
            pollingTimeout = 30000,
            syncRooms = response.rooms,
        ))
    }

    private suspend fun onSyncError(scope: CoroutineScope, error: Throwable?) {
        store.intent(OnSyncError)

        error?.let { logError(TAG, it) }
        if (store.state().pollingRetries >= BeaconConfig.matrixMaxSyncRetries) {
            logDebug(TAG, MESSAGE_SYNC_RETRIES_EXCEEDED)
            scope.cancel()
        } else {
            logDebug(TAG, MESSAGE_RETRY_SYNCING)
        }
    }

    private suspend fun createTxnId(): String {
        val timestamp = currentTimestamp()
        val counter = store.state().transactionCounter

        store.intent(OnTxnIdCreated)

        return "m$timestamp.$counter"
    }

    private fun failWithRequiresAuthorization(name: String): Nothing =
        throw IllegalStateException("$ERROR_MESSAGE_AUTHORIZATION_REQUIRED ($name)")

    companion object {
        const val TAG = "MatrixClient"

        private const val MESSAGE_RETRY_SYNCING = "Retry syncing..."
        private const val MESSAGE_SYNC_RETRIES_EXCEEDED = "Max sync retries exceeded"

        private const val ERROR_MESSAGE_LOGIN_FAILED = "Login failed"
        private const val ERROR_MESSAGE_AUTHORIZATION_REQUIRED = "Authorization required but no access token has been provided"
    }
}