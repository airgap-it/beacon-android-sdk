package it.airgap.beaconsdk.core.internal.storage

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.beacon.AppMetadata
import it.airgap.beaconsdk.core.data.beacon.Peer
import it.airgap.beaconsdk.core.data.beacon.Permission
import kotlinx.coroutines.flow.Flow

@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface ExtendedStorage : Storage {
    public val appMetadata: Flow<AppMetadata>
    public val permissions: Flow<Permission>
    public val peers: Flow<Peer>

    public suspend fun addPeers(
        peers: List<Peer>,
        overwrite: Boolean = false,
        compare: (Peer, Peer) -> Boolean = { first, second -> first == second },
    )

    public suspend fun findPeer(predicate: (Peer) -> Boolean): Peer?
    public suspend fun <T : Peer> findPeer(instanceClass: Class<T>, predicate: (T) -> Boolean): T?
    public suspend fun removePeers(predicate: ((Peer) -> Boolean)? = null)

    public suspend fun addAppMetadata(
        appsMetadata: List<AppMetadata>,
        overwrite: Boolean = false,
        compare: (AppMetadata, AppMetadata) -> Boolean = { first, second -> first == second },
    )

    public suspend fun findAppMetadata(predicate: (AppMetadata) -> Boolean): AppMetadata?
    public suspend fun removeAppMetadata(predicate: ((AppMetadata) -> Boolean)? = null)

    public suspend fun addPermissions(
        permissions: List<Permission>,
        overwrite: Boolean = false,
        compare: (Permission, Permission) -> Boolean = { first, second -> first == second },
    )

    public suspend fun findPermission(predicate: (Permission) -> Boolean): Permission?
    public suspend fun removePermissions(predicate: ((Permission) -> Boolean)? = null)

    public suspend fun removeMatrixRelayServer()
    public suspend fun removeMatrixChannels()
    public suspend fun removeMatrixSyncToken()
    public suspend fun removeMatrixRooms()

    public suspend fun addMigrations(migrations: Set<String>)

    override fun extend(): ExtendedStorage = this
}