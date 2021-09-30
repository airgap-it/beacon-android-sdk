package it.airgap.beaconsdk.core.storage

import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.Peer
import it.airgap.beaconsdk.core.data.Permission
import kotlinx.coroutines.flow.Flow

public interface ExtendedStorage : Storage {
    public val appMetadata: Flow<AppMetadata>
    public val permissions: Flow<Permission>
    public val peers: Flow<Peer>

    // -- Beacon --

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

    // -- SDK --

    public suspend fun addMigrations(migrations: Set<String>)

    override fun extend(): ExtendedStorage = this
}