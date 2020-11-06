package it.airgap.beaconsdk.internal.storage

import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.P2pPeerInfo
import it.airgap.beaconsdk.data.beacon.PermissionInfo
import kotlinx.coroutines.flow.Flow

internal interface ExtendedStorage : Storage {
    val appMetadata: Flow<AppMetadata>
    val permissions: Flow<PermissionInfo>
    val p2pPeers: Flow<P2pPeerInfo>

    suspend fun addP2pPeers(
        peers: List<P2pPeerInfo>,
        overwrite: Boolean = false,
        compare: (P2pPeerInfo, P2pPeerInfo) -> Boolean = { first, second -> first == second },
    )

    suspend fun findP2pPeer(predicate: (P2pPeerInfo) -> Boolean): P2pPeerInfo?
    suspend fun removeP2pPeers(predicate: ((P2pPeerInfo) -> Boolean)? = null)

    suspend fun addAppMetadata(
        appsMetadata: List<AppMetadata>,
        overwrite: Boolean = false,
        compare: (AppMetadata, AppMetadata) -> Boolean = { first, second -> first == second },
    )

    suspend fun findAppMetadata(predicate: (AppMetadata) -> Boolean): AppMetadata?
    suspend fun removeAppMetadata(predicate: ((AppMetadata) -> Boolean)? = null)

    suspend fun addPermissions(
        permissions: List<PermissionInfo>,
        overwrite: Boolean = false,
        compare: (PermissionInfo, PermissionInfo) -> Boolean = { first, second -> first == second },
    )

    suspend fun findPermission(predicate: (PermissionInfo) -> Boolean): PermissionInfo?
    suspend fun removePermissions(predicate: ((PermissionInfo) -> Boolean)? = null)

    override fun extend(): ExtendedStorage = this
}