package it.airgap.beaconsdk.internal.storage.decorator

import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.P2pPeerInfo
import it.airgap.beaconsdk.data.beacon.PermissionInfo
import it.airgap.beaconsdk.internal.storage.ExtendedStorage
import it.airgap.beaconsdk.internal.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

internal class DecoratedExtendedStorage(private val storage: ExtendedStorage) : ExtendedStorage by storage {
    constructor(storage: Storage) : this(storage.extend())

    private val removedP2pPeerInfo: MutableSharedFlow<P2pPeerInfo> by lazy { MutableSharedFlow(extraBufferCapacity = 64) }
    val updatedP2pPeers: Flow<P2pPeerInfo>
        get() = merge(storage.p2pPeers, removedP2pPeerInfo)

    suspend fun removeP2pPeers(peers: List<P2pPeerInfo>) {
        removeP2pPeers { peer -> peers.any { it.publicKey == peer.publicKey } }
    }

    override suspend fun removeP2pPeers(predicate: ((P2pPeerInfo) -> Boolean)?) {
        val peers = storage.getP2pPeers()
        val toRemove = predicate?.let { peers.filter(it) } ?: peers

        storage.removeP2pPeers(predicate)
        removePermissions { permission -> toRemove.any { it.publicKey == permission.appMetadata.senderId } }

        CoroutineScope(Dispatchers.Default).launch {
            toRemove.map { it.copy(isRemoved = true) }.forEach {
                removedP2pPeerInfo.tryEmit(it)
            }
        }
    }

    suspend fun removeAppMetadata(appsMetadata: List<AppMetadata>) {
        removeAppMetadata { metadata -> appsMetadata.any { it.senderId == metadata.senderId } }
    }

    suspend fun removePermissions(permissions: List<PermissionInfo>) {
        removePermissions { permission -> permissions.any { it == permission } }
    }
}