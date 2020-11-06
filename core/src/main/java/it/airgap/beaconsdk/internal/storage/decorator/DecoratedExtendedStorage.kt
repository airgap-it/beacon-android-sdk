package it.airgap.beaconsdk.internal.storage.decorator

import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.P2pPeerInfo
import it.airgap.beaconsdk.data.beacon.PermissionInfo
import it.airgap.beaconsdk.internal.storage.ExtendedStorage
import it.airgap.beaconsdk.internal.storage.Storage

internal class DecoratedExtendedStorage(private val storage: ExtendedStorage) : ExtendedStorage by storage {
    constructor(storage: Storage) : this(storage.extend())

    suspend fun removeP2pPeers(peers: List<P2pPeerInfo>) {
        removeP2pPeers { peer -> peers.any { it.publicKey == peer.publicKey } }
        removePermissions { permission -> peers.any { it.publicKey == permission.appMetadata.senderId } }
    }

    suspend fun removeAppMetadata(appsMetadata: List<AppMetadata>) {
        removeAppMetadata { metadata -> appsMetadata.any { it.senderId == metadata.senderId } }
    }

    suspend fun removePermissions(permissions: List<PermissionInfo>) {
        removePermissions { permission -> permissions.any { it == permission } }
    }
}