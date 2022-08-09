package it.airgap.beaconsdk.core.internal.transport.p2p.store

import it.airgap.beaconsdk.core.internal.base.Store
import it.airgap.beaconsdk.core.internal.utils.*
import kotlinx.coroutines.CompletableDeferred

internal class P2pTransportStore : Store<P2pTransportStoreState, P2pTransportStoreAction>() {

    private var state: P2pTransportStoreState = P2pTransportStoreState()
    override suspend fun stateLocked(): Result<P2pTransportStoreState> = Result.success(state)

    override suspend fun intentLocked(action: P2pTransportStoreAction): Result<Unit> =
        runCatching {
            state = when (action) {
                OnPairingRequested -> onPairingRequested()
                is OnPairingCompleted -> onPairingCompleted(action)
                DiscardPairingData -> onDiscardPairingData()
            }
        }

    private fun onPairingRequested(): P2pTransportStoreState =
        state.copy(pairingPeerDeferred = CompletableDeferred())

    private fun onPairingCompleted(action: OnPairingCompleted): P2pTransportStoreState =
        state.also { it.pairingPeerDeferred?.complete(action.peer) }

    private fun onDiscardPairingData(): P2pTransportStoreState =
        state.copy(pairingPeerDeferred = null)

    companion object {
        const val TAG = "P2pTransportStore"
    }
}