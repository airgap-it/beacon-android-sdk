package it.airgap.beaconsdk.internal.transport.p2p.matrix.store

import it.airgap.beaconsdk.internal.storage.ExtendedStorage
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.client.MatrixEvent
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.client.MatrixRoom
import it.airgap.beaconsdk.internal.transport.p2p.matrix.store.MatrixStoreAction.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class MatrixStore(private val storage: ExtendedStorage) {
    private val _events: MutableSharedFlow<MatrixEvent> = MutableSharedFlow(extraBufferCapacity = 64)
    val events: Flow<MatrixEvent>
        get() = _events

    private var state: MatrixState = MatrixState()
    private val stateMutex: Mutex = Mutex()

    suspend fun state(): MatrixState = stateMutex.withLock { state }

    suspend fun intent(action: MatrixStoreAction) {
        stateMutex.withLock {
            state = when (action) {
                is Init -> {
                    val syncToken = storage.getMatrixSyncToken()
                    val rooms = storage.getMatrixRooms()

                    state.copy(
                        userId = action.userId,
                        deviceId = action.deviceId,
                        accessToken = action.accessToken,
                        syncToken = syncToken,
                        rooms = rooms.associateBy { it.id }
                    )
                }
                is OnSyncSuccess -> {
                    val newRooms = action.syncRooms?.let { MatrixRoom.fromSync(it) }
                    val events = action.syncRooms?.let { MatrixEvent.fromSync(it) }

                    val mergedRooms = newRooms?.ifNotEmpty { state.rooms.merge(it) }

                    events?.filterNotNull()?.forEach { _events.tryEmit(it) }

                    with (storage) {
                        action.syncToken?.let { setMatrixSyncToken(it) }
                        mergedRooms?.values?.toList()?.let { setMatrixRooms(it) }
                    }

                    state.copy(
                        isPolling = true,
                        syncToken = action.syncToken,
                        pollingTimeout = action.pollingTimeout,
                        pollingRetries = 0,
                        rooms = mergedRooms ?: state.rooms,
                    )
                }
                is OnSyncError -> {
                    state.copy(
                        isPolling = false,
                        pollingRetries = state.pollingRetries + 1
                    )
                }
                is OnTxnIdCreated -> {
                    state.copy(transactionCounter = state.transactionCounter + 1)
                }
            }
        }
    }

    private fun Map<String, MatrixRoom>.merge(newRooms: List<MatrixRoom>): Map<String, MatrixRoom> {
        val new = newRooms.map { get(it.id)?.update(it.members) ?: it }

        return (values + new).associateBy { it.id }
    }

    private fun Map<String, MatrixRoom>.merge(vararg newRooms: MatrixRoom): Map<String, MatrixRoom> {
        val new = newRooms.map { get(it.id)?.update(it.members) ?: it }

        return (values + new).associateBy { it.id }
    }

    private fun <T, R> List<T>.ifNotEmpty(block: (List<T>) -> R): R? =
        if (!isNullOrEmpty()) block(this)
        else null
}