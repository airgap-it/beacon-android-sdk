package it.airgap.beaconsdk.core.internal.storage

import it.airgap.beaconsdk.core.data.beacon.AppMetadata
import it.airgap.beaconsdk.core.data.beacon.Peer
import it.airgap.beaconsdk.core.data.beacon.Permission
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
    suspend fun <T : Peer> findPeer(instanceClass: Class<T>, predicate: (T) -> Boolean): T?
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

    suspend fun removeMatrixRelayServer()
    suspend fun removeMatrixChannels()
    suspend fun removeMatrixSyncToken()
    suspend fun removeMatrixRooms()

    suspend fun addMigrations(migrations: Set<String>)

    override fun extend(): ExtendedStorage = this
}