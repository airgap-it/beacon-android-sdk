package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.store

import it.airgap.beaconsdk.core.internal.base.Store
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.delegate.disposable
import it.airgap.beaconsdk.core.internal.utils.success
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.MatrixEvent
import it.airgap.beaconsdk.transport.p2p.matrix.data.MatrixRoom
import it.airgap.beaconsdk.transport.p2p.matrix.internal.storage.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

internal class MatrixStore(private val storageManager: StorageManager) : Store<MatrixStoreState, MatrixStoreAction>() {
    private var initialEvents: List<MatrixEvent>? by disposable()
    private val _events: MutableSharedFlow<MatrixEvent> = MutableSharedFlow(extraBufferCapacity = 64)
    val events: Flow<MatrixEvent>
        get() = _events.onStart { initialEvents?.forEach { emit(it) }  }

    private var state: MatrixStoreState = MatrixStoreState()
    override suspend fun stateLocked(): Result<MatrixStoreState> = Result.success(state)

    override suspend fun intentLocked(action: MatrixStoreAction): Result<Unit> {
        state = when (action) {
            is Init -> stateInit(action)
            is OnSyncSuccess -> stateOnSyncSuccess(action)
            is OnSyncError -> stateOnSyncError()
            is OnTxnIdCreated -> stateOnTxnIdCreated()
            is Reset -> stateReset()
            is HardReset -> stateHardReset()
        }

        return Result.success()
    }

    private suspend fun stateInit(action: Init): MatrixStoreState {
        val syncToken = storageManager.getMatrixSyncToken()
        val rooms = storageManager.getMatrixRooms()

        return state.copy(
            userId = action.userId,
            deviceId = action.deviceId,
            accessToken = action.accessToken,
            syncToken = syncToken,
            rooms = rooms.associateBy { it.id }
        )
    }

    private suspend fun stateOnSyncSuccess(action: OnSyncSuccess): MatrixStoreState {
        val mergedRooms = action.rooms.ifNotNullOrEmpty { state.rooms.merge(it) }

        action.events?.let { _events.tryEmit(it) }

        with(storageManager) {
            action.syncToken?.let { setMatrixSyncToken(it) }
            mergedRooms?.values?.toList()?.let { setMatrixRooms(it) }
        }

        return state.copy(
            isPolling = true,
            syncToken = action.syncToken,
            pollingTimeout = action.pollingTimeout,
            pollingRetries = 0,
            rooms = mergedRooms ?: state.rooms,
        )
    }

    private fun stateOnSyncError(): MatrixStoreState =
        state.copy(
            isPolling = false,
            pollingRetries = state.pollingRetries + 1
        )

    private fun stateOnTxnIdCreated(): MatrixStoreState =
        state.copy(transactionCounter = state.transactionCounter + 1)

    private fun stateReset(): MatrixStoreState =
        MatrixStoreState(syncToken = state.syncToken)

    private suspend fun stateHardReset(): MatrixStoreState = coroutineScope {
        launch {
            storageManager.removeMatrixRooms()
        }

        stateReset()
    }

    private fun MutableSharedFlow<MatrixEvent>.tryEmit(events: List<MatrixEvent>) {
        if (subscriptionCount.value == 0) {
            initialEvents = events
        }

        events.forEach { tryEmit(it) }
    }

    private fun Map<String, MatrixRoom>.merge(newRooms: List<MatrixRoom>): Map<String, MatrixRoom> {
        val new = newRooms.map { get(it.id)?.update(it.members) ?: it }

        return (values + new).associateBy { it.id }
    }

    private fun <T, R> List<T>?.ifNotNullOrEmpty(block: (List<T>) -> R): R? =
        if (!isNullOrEmpty()) block(this)
        else null
}