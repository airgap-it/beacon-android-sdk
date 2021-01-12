package it.airgap.beaconsdk.internal.storage

import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.Peer
import it.airgap.beaconsdk.data.beacon.Permission
import kotlinx.coroutines.flow.Flow

internal interface ExtendedStorage : Storage {
    val appMetadata: Flow<AppMetadata>
    val permissions: Flow<Permission>
    val peers: Flow<Peer>

    suspend fun addPeers(
        peers: List<Peer>,
        overwrite: Boolean = false,
        compare: (Peer, Peer) -> Boolean = { first, second -> first == second },
    )

    suspend fun findPeer(predicate: (Peer) -> Boolean): Peer?
    suspend fun removePeers(predicate: ((Peer) -> Boolean)? = null)

    suspend fun addAppMetadata(
        appsMetadata: List<AppMetadata>,
        overwrite: Boolean = false,
        compare: (AppMetadata, AppMetadata) -> Boolean = { first, second -> first == second },
    )

    suspend fun findAppMetadata(predicate: (AppMetadata) -> Boolean): AppMetadata?
    suspend fun removeAppMetadata(predicate: ((AppMetadata) -> Boolean)? = null)

    suspend fun addPermissions(
        permissions: List<Permission>,
        overwrite: Boolean = false,
        compare: (Permission, Permission) -> Boolean = { first, second -> first == second },
    )

    suspend fun findPermission(predicate: (Permission) -> Boolean): Permission?
    suspend fun removePermissions(predicate: ((Permission) -> Boolean)? = null)

    override fun extend(): ExtendedStorage = this
}