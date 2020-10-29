package it.airgap.beaconsdk.internal.storage.decorator

import it.airgap.beaconsdk.data.beacon.AccountInfo
import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.P2pPeerInfo
import it.airgap.beaconsdk.data.beacon.PermissionInfo
import it.airgap.beaconsdk.internal.storage.ExtendedStorage
import it.airgap.beaconsdk.internal.storage.Storage

internal class DecoratedExtendedStorage(private val storage: ExtendedStorage) : ExtendedStorage by storage {
    constructor(storage: Storage) : this(storage.extend())

    suspend fun addP2pPeers(
        vararg peers: P2pPeerInfo,
        overwrite: Boolean = false,
        compare: (P2pPeerInfo, P2pPeerInfo) -> Boolean = { first, second -> first == second },
    ) {
        addP2pPeers(peers.toList(), overwrite, compare)
    }

    suspend fun removeP2pPeers(peers: List<P2pPeerInfo>) {
        val removed =
            if (peers.isEmpty()) getP2pPeers().also { removeP2pPeers() }
            else peers.also { removeP2pPeers { peer -> peers.any { it.publicKey == peer.publicKey } } }

        removePermissions { permission -> removed.any { it.publicKey == permission.appMetadata.senderId } }
    }

    suspend fun addAccounts(
        vararg accounts: AccountInfo,
        overwrite: Boolean = false,
        compare: (AccountInfo, AccountInfo) -> Boolean = { first, second -> first == second },
    ) {
        addAccounts(accounts.toList(), overwrite, compare)
    }

    suspend fun addAppsMetadata(
        vararg appsMetadata: AppMetadata,
        overwrite: Boolean = false,
        compare: (AppMetadata, AppMetadata) -> Boolean = { first, second -> first == second },
    ) {
        addAppsMetadata(appsMetadata.toList(), overwrite, compare)
    }

    suspend fun removeAppsMetadata(appsMetadata: List<AppMetadata>) {
        if (appsMetadata.isEmpty()) removeAppsMetadata()
        else removeAppsMetadata { metadata -> appsMetadata.any { it.senderId == metadata.senderId } }
    }

    suspend fun addPermissions(
        vararg permissions: PermissionInfo,
        overwrite: Boolean = false,
        compare: (PermissionInfo, PermissionInfo) -> Boolean = { first, second -> first == second },
    ) {
        addPermissions(permissions.toList(), overwrite, compare)
    }

    suspend fun removePermissions(permissions: List<PermissionInfo>) {
        if (permissions.isEmpty()) removePermissions()
        else removePermissions { permission -> permissions.any { it == permission } }
    }
}