package it.airgap.client.dapp.internal.controller.account.store

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.data.P2pPeer
import it.airgap.beaconsdk.core.data.Peer
import it.airgap.beaconsdk.core.internal.base.Store
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.client.dapp.internal.storage.*
import it.airgap.client.dapp.internal.storage.getActiveAccount
import it.airgap.client.dapp.internal.storage.getActivePeer
import it.airgap.client.dapp.internal.storage.removeActivePeer
import it.airgap.client.dapp.internal.storage.setActivePeer

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AccountControllerStore(private val storageManager: StorageManager) : Store<AccountControllerStoreState, AccountControllerStoreAction>() {

    private var state: AccountControllerStoreState? = null
    override suspend fun stateLocked(): Result<AccountControllerStoreState> = withState { it }

    override suspend fun intentLocked(action: AccountControllerStoreAction): Result<Unit> =
        runCatching {
            state = when (action) {
                is OnPeerPaired -> onPeerPaired(action).getOrThrow()
                is OnPeerRemoved -> onPeerRemoved(action).getOrThrow()
                ResetActivePeer -> resetActivePeer().getOrThrow()
                is OnNewActiveAccount -> onNewActiveAccount(action).getOrThrow()
                ResetActiveAccount -> resetActiveAccount().getOrThrow()
                HardReset -> reset().getOrThrow()
            }
        }

    private suspend fun onPeerPaired(action: OnPeerPaired): Result<AccountControllerStoreState> =
        withState { state ->
            val activePeer = getAndUpdateActivePeer(state.activePeer, action.peer).getOrThrow()

            state.copy(activePeer = activePeer)
        }

    private suspend fun onPeerRemoved(action: OnPeerRemoved): Result<AccountControllerStoreState> =
        withState { state ->
            if (action.peer.publicKey == state.activePeer?.publicKey) reset().getOrThrow()
            else state
        }

    private suspend fun resetActivePeer(): Result<AccountControllerStoreState> =
        withState { state ->
            state.copy(activePeer = null).also { storageManager.removeActivePeer() }
        }

    private suspend fun onNewActiveAccount(action: OnNewActiveAccount): Result<AccountControllerStoreState> =
        withState { state ->
            storageManager.setActiveAccount(action.account)

            val peer = if (action.account.peerId != state.activePeer?.publicKey) {
                storageManager.findPeer { it.publicKey == action.account.peerId }
            } else state.activePeer

            val activePeer = getAndUpdateActivePeer(state.activePeer, peer).getOrThrow()

            state.copy(
                activeAccount = action.account,
                activePeer = activePeer,
            )
        }

    private suspend fun resetActiveAccount(): Result<AccountControllerStoreState> =
        withState { state ->
            state.copy(activeAccount = null).also { storageManager.removeActiveAccount() }
        }

    private suspend fun reset(): Result<AccountControllerStoreState> =
        withState { state ->
            state.copy(
                activeAccount = null,
                activePeer = null,
            ).also {
                storageManager.removeActiveAccount()
                storageManager.removeActivePeer()
            }
        }

    private suspend fun getAndUpdateActivePeer(activePeer: Peer?, newPeer: Peer?): Result<Peer?> =
        runCatching {
            if (activePeer?.publicKey == newPeer?.publicKey) activePeer
            else {
                storageManager.setActivePeer(newPeer?.publicKey)
                val newActivePeer = storageManager.findPeer { it.publicKey == newPeer?.publicKey }

                newActivePeer ?: newPeer?.also { storageManager.addPeers(listOf(it)) }
            }
        }

    private suspend inline fun <T> withState(action: (state: AccountControllerStoreState) -> T): Result<T> {
        val state = runCatching {
            state ?: createState().getOrThrow().also { state = it }
        }
        return state.map(action)
    }

    private suspend fun createState(): Result<AccountControllerStoreState> =
        runCatching {
            val activeAccount = storageManager.getActiveAccount()
            val activePeerId = storageManager.getActivePeer(default = activeAccount?.peerId)

            val activePeer = activePeerId?.let { activePeer -> storageManager.findPeer { it.publicKey == activePeer } }

            AccountControllerStoreState(
                activeAccount = activeAccount,
                activePeer = activePeer,
            )
        }

    private suspend fun StorageManager.getActivePeer(default: String?): String? =
        getActivePeer()?.takeIf { it == default } ?: default?.also { setActivePeer(it) }

    public companion object {}
}