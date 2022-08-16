package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix

import it.airgap.beaconsdk.core.internal.utils.*
import it.airgap.beaconsdk.transport.p2p.matrix.data.MatrixRoom
import it.airgap.beaconsdk.transport.p2p.matrix.internal.BeaconP2pMatrixConfiguration
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.MatrixEvent
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.MatrixSync
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.room.MatrixCreateRoomRequest
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.event.MatrixEventService
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.node.MatrixNodeService
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.room.MatrixRoomService
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.user.MatrixUserService
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.store.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex

internal class MatrixClient(
    private val store: MatrixStore,
    private val nodeService: MatrixNodeService,
    private val userService: MatrixUserService,
    private val roomService: MatrixRoomService,
    private val eventService: MatrixEventService,
    private val poller: Poller,
) {
    val events: Flow<MatrixEvent>
        get() = store.events

    private val syncScopes: MutableMap<String, CoroutineScope> = mutableMapOf()

    suspend fun joinedRooms(): List<MatrixRoom.Joined> =
        store.state().getOrNull()?.rooms?.values?.filterIsInstance<MatrixRoom.Joined>() ?: emptyList()

    suspend fun invitedRooms(): List<MatrixRoom.Invited> =
        store.state().getOrNull()?.rooms?.values?.filterIsInstance<MatrixRoom.Invited>() ?: emptyList()

    suspend fun leftRooms(): List<MatrixRoom.Left> =
        store.state().getOrNull()?.rooms?.values?.filterIsInstance<MatrixRoom.Left>() ?: emptyList()

    suspend fun isLoggedIn(): Boolean = store.state().getOrNull()?.accessToken != null

    suspend fun isUp(node: String): Boolean = nodeService.isUp(node)

    suspend fun start(node: String, userId: String, password: String, deviceId: String): Result<Unit> =
        runCatching {
            val loginResponse = userService.login(node, userId, password, deviceId)

            val accessToken = loginResponse.getOrNull()?.accessToken
                ?: failWith("Login failed", loginResponse.exceptionOrNull())

            store.intent(Init(userId, deviceId, accessToken))
            syncScope(node) { syncPoll(it, node).collect() }
        }

    suspend fun stop(node: String? = null) {
        with(syncScopes) {
            if (node != null) {
                get(node)?.cancel("Sync for node $node canceled.")
                remove(node)
            } else {
                forEach { it.value.cancel("Sync for node ${it.key} canceled.") }
                clear()
            }

            if (isEmpty()) {
                store.intent(Reset)
            }
        }
    }

    suspend fun resetHard(node: String? = null) {
        stop(node)
        store.intent(HardReset)
    }

    suspend fun createTrustedPrivateRoom(node: String, vararg members: String): Result<MatrixRoom?> =
        runCatchingFlat {
            withAccessToken("createTrustedPrivateRoom") { accessToken ->
                roomService.createRoom(
                    node,
                    accessToken,
                    MatrixCreateRoomRequest(
                        invite = members.toList(),
                        preset = MatrixCreateRoomRequest.Preset.TrustedPrivateChat,
                        isDirect = true,
                        roomVersion = BeaconP2pMatrixConfiguration.MATRIX_ROOM_VERSION,
                    ),
                ).map { response -> response.roomId?.let { MatrixRoom.Unknown(it) } }
            }
        }

    suspend fun inviteToRoom(node: String, user: String, roomId: String): Result<Unit> =
        runCatching {
            withAccessToken("inviteToRooms") {
                roomService.inviteToRoom(node, it, user, roomId).getOrThrow()
            }
        }

    suspend fun inviteToRoom(node: String, user: String, room: MatrixRoom): Result<Unit> =
        runCatching {
            withAccessToken("inviteToRooms") {
                roomService.inviteToRoom(node, it, user, room.id).getOrThrow()
            }
        }

    suspend fun joinRoom(node: String, roomId: String): Result<Unit> =
        runCatching {
            withAccessToken("joinRooms") {
                roomService.joinRoom(node, it, roomId).getOrThrow()
            }
        }

    suspend fun joinRoom(node: String, room: MatrixRoom): Result<Unit> =
        runCatching {
            withAccessToken("joinRooms") {
                roomService.joinRoom(node, it, room.id).getOrThrow()
            }
        }

    suspend fun sendTextMessage(node: String, roomId: String, message: String): Result<Unit> =
        runCatching {
            withAccessToken("sendTextMessage") { accessToken ->
                eventService.sendTextMessage(
                    node,
                    accessToken,
                    roomId,
                    createTxnId(),
                    message,
                ).getOrThrow()
            }
        }

    suspend fun sendTextMessage(node: String, room: MatrixRoom, message: String): Result<Unit> =
        runCatching {
            withAccessToken("sendTextMessage") { accessToken ->
                eventService.sendTextMessage(
                    node,
                    accessToken,
                    room.id,
                    createTxnId(),
                    message,
                ).getOrThrow()
            }
        }

    suspend fun sync(node: String): Result<MatrixSync> =
        runCatchingFlat {
            val state = store.state().getOrThrow()

            withAccessToken("sync") { accessToken ->
                eventService
                    .sync(node, accessToken, state.syncToken, state.pollingTimeout)
                    .map { MatrixSync.fromSyncResponse(node, it) }
            }
        }

    fun syncPoll(scope: CoroutineScope, node: String, interval: Long = 0): Flow<Result<MatrixSync>> {
        val syncMutex = Mutex()

        return poller.poll(Dispatchers.IO, interval) {
            syncMutex.lock()
            sync(node)
        }.onEach { result ->
            result
                .onSuccess { onSyncSuccess(it) }
                .onFailure { onSyncError(scope, it) }

            if (syncMutex.isLocked) syncMutex.unlock()
        }
    }

    private suspend inline fun <T> withAccessToken(
        name: String,
        block: (accessToken: String) -> T,
    ): T {
        val accessToken = store.state().getOrNull()?.accessToken ?: failWithRequiresAuthorization(name)
        return block(accessToken)
    }

    private suspend fun syncScope(node: String, block: suspend (CoroutineScope) -> Unit) {
        syncScopes
            .getOrPut(node) { CoroutineScope(CoroutineName(syncScopeName(node)) + Dispatchers.Default) }
            .launch {
                block(this)
                syncScopes.remove(node)
            }
    }

    private fun syncScopeName(node: String): String = "sync@$node"

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

        val pollingRetries = store.state().getOrNull()?.pollingRetries
        if (pollingRetries == null || pollingRetries >= BeaconP2pMatrixConfiguration.MATRIX_MAX_SYNC_RETRIES) {
            logDebug(TAG, "Max sync retries exceeded")
            scope.cancel()
        } else {
            logDebug(TAG, "Retry syncing...")
        }
    }

    private suspend fun createTxnId(): String {
        val timestamp = currentTimestamp()
        val counter = store.state().getOrThrow().transactionCounter

        store.intent(OnTxnIdCreated)

        return "m$timestamp.$counter"
    }

    private fun failWithRequiresAuthorization(name: String): Nothing =
        throw IllegalStateException("Authorization required but no access token has been provided ($name)")

    companion object {
        const val TAG = "MatrixClient"
    }
}