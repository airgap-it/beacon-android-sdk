package it.airgap.beaconsdk.transport.p2p.matrix.internal.store

import it.airgap.beaconsdk.core.data.P2pPeer
import it.airgap.beaconsdk.core.internal.base.Store
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.migration.Migration
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.transport.p2p.data.p2pIdentifierOrNull
import it.airgap.beaconsdk.core.internal.utils.*
import it.airgap.beaconsdk.core.storage.findPeer
import it.airgap.beaconsdk.transport.p2p.matrix.internal.P2pMatrixCommunicator
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.MatrixClient
import it.airgap.beaconsdk.transport.p2p.matrix.internal.migration.migrateMatrixRelayServer
import it.airgap.beaconsdk.transport.p2p.matrix.internal.storage.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal class P2pMatrixStore(
    private val app: BeaconApplication,
    private val communicator: P2pMatrixCommunicator,
    private val matrixClient: MatrixClient,
    private val matrixNodes: List<String>,
    private val storageManager: StorageManager,
    private val migration: Migration,
) : Store<P2pMatrixStoreState, P2pMatrixStoreAction>() {

    private var state: P2pMatrixStoreState? = null
    override suspend fun stateLocked(): Result<P2pMatrixStoreState> = withState { it }

    override suspend fun intentLocked(action: P2pMatrixStoreAction): Result<Unit> =
        runCatching {
            state = when (action) {
                is OnChannelCreated -> stateOnChannelCreated(action).getOrThrow()
                is OnChannelEvent -> stateOnChannelEvent(action).getOrThrow()
                is OnChannelClosed -> stateOnChannelClosed(action).getOrThrow()
                is HardReset -> stateHardReset()
            }
        }

    private suspend fun stateOnChannelCreated(action: OnChannelCreated): Result<P2pMatrixStoreState> =
        stateOnActiveChannelUpdate(action.recipient, action.channelId)

    private suspend fun stateOnChannelEvent(action: OnChannelEvent): Result<P2pMatrixStoreState> =
        runCatchingFlat {
            updatePeerRelayServer(action.sender)
            stateOnActiveChannelUpdate(action.sender, action.channelId)
        }

    private suspend fun stateOnChannelClosed(action: OnChannelClosed): Result<P2pMatrixStoreState> =
        withState { state ->
            val activeChannels = state.activeChannels.filterValues { it != action.channelId }
            val inactiveChannels = state.inactiveChannels + action.channelId

            storageManager.setMatrixChannels(activeChannels)

            if (activeChannels == state.activeChannels && inactiveChannels == state.inactiveChannels) state
            else state.copy(activeChannels = activeChannels, inactiveChannels = inactiveChannels)
        }

    private suspend fun stateHardReset(): P2pMatrixStoreState? = coroutineScope {
        launch {
            with(storageManager) {
                removeMatrixRelayServer()
                removeMatrixChannels()
            }
        }

        null
    }

    private suspend fun stateOnActiveChannelUpdate(user: String, channelId: String): Result<P2pMatrixStoreState> =
        withState { state ->
            val (activeChannels, inactiveChannels) = updateActiveChannels(state, user, channelId)

            if (activeChannels == state.activeChannels && inactiveChannels == state.inactiveChannels) state
            else state.copy(activeChannels = activeChannels, inactiveChannels = inactiveChannels)
        }

    private suspend fun updateActiveChannels(state: P2pMatrixStoreState, user: String, channelId: String): Pair<Map<String, String>, Set<String>> {
        val assignedChannel = state.activeChannels[user]
        if (assignedChannel == channelId) return Pair(state.activeChannels, state.inactiveChannels - channelId)

        val activeChannels = state.activeChannels + (user to channelId)
        val inactiveChannels = (
            if (assignedChannel != null) state.inactiveChannels + assignedChannel
            else state.inactiveChannels
        ) - channelId

        storageManager.setMatrixChannels(activeChannels)

        return Pair(activeChannels, inactiveChannels)
    }

    private suspend fun updatePeerRelayServer(sender: String) {
        val senderIdentifier = p2pIdentifierOrNull(sender) ?: return

        val peer = storageManager.findPeer<P2pPeer> {
            runCatching {
                val peerIdentifier = communicator.recipientIdentifier(
                    it.publicKey.asHexString().toByteArray(),
                    it.relayServer
                ).getOrThrow()

                val publicKeysMatch = senderIdentifier.publicKeyHash == peerIdentifier.publicKeyHash
                val relayServersMatch = senderIdentifier.relayServer == peerIdentifier.relayServer

                publicKeysMatch && !relayServersMatch
            }.getOrDefault(false)
        } ?: return

        storageManager.addPeers(listOf(peer.copy(relayServer = senderIdentifier.relayServer)), overwrite = true) { listOf(publicKey) }
    }

    private suspend inline fun <T> withState(action: (state: P2pMatrixStoreState) -> T): Result<T> {
        val state = runCatching {
            state ?: createState().getOrThrow().also { state = it }
        }
        return state.map(action)
    }

    private suspend fun createState(): Result<P2pMatrixStoreState> =
        runCatching {
            P2pMatrixStoreState(
                relayServer = relayServer().getOrThrow(),
                availableNodes = matrixNodes.size,
                activeChannels = storageManager.getMatrixChannels(),
            )
        }

    private suspend fun relayServer(): Result<String> =
        runCatching runCatching@ {
            migration.migrateMatrixRelayServer(matrixNodes)

            logDebug(TAG, "Looking for a Matrix relay server...")

            val savedRelayServer = storageManager.getMatrixRelayServer()
            savedRelayServer?.let {
                logDebug(TAG, "Found cached Matrix relay server, using $it.")

                return@runCatching it
            }

            logDebug(TAG, "No cached Matrix relay server found, looking for reachable nodes...")

            val offset = app.publicKeyIndex(matrixNodes.size)
            val upRelayServer = matrixNodes
                .shiftedBy(offset)
                .firstOrNull { node ->
                    matrixClient.isUp(node).also {
                        if (!it) logDebug(TAG, "Node $node is unreachable.")
                    }
                } ?: failWithUnreachableNodes()

            logDebug(TAG, "Using $upRelayServer.")

            storageManager.setMatrixRelayServer(upRelayServer)

            return@runCatching upRelayServer
        }

    private fun BeaconApplication.publicKeyIndex(boundary: Int): Int =
        keyPair.publicKey.fold(0) { acc, byte -> acc + byte.toUByte().toInt() } % boundary

    private fun failWithUnreachableNodes(): Nothing = failWith("No reachable Matrix node found.")

    companion object {
        const val TAG = "P2pMatrixStore"
    }
}