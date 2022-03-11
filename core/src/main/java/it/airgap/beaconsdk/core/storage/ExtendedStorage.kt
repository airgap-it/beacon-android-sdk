package it.airgap.beaconsdk.core.storage

import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.Peer
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

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
    public suspend fun <T : Peer> findPeer(instanceClass: KClass<T>, predicate: (T) -> Boolean): T?
    public suspend fun removePeers(predicate: ((Peer) -> Boolean)? = null)

    public suspend fun addAppMetadata(
        appsMetadata: List<AppMetadata>,
        overwrite: Boolean = false,
        compare: (AppMetadata, AppMetadata) -> Boolean = { first, second -> first == second },
    )

    public suspend fun findAppMetadata(predicate: (AppMetadata) -> Boolean): AppMetadata?
    public suspend fun <T : AppMetadata> findAppMetadata(instanceClass: KClass<T>, predicate: (T) -> Boolean): T?
    public suspend fun removeAppMetadata(predicate: ((AppMetadata) -> Boolean)? = null)

    public suspend fun addPermissions(
        permissions: List<Permission>,
        overwrite: Boolean = false,
        compare: (Permission, Permission) -> Boolean = { first, second -> first == second },
    )

    public suspend fun findPermission(predicate: (Permission) -> Boolean): Permission?
    public suspend fun <T : Permission> findPermission(instanceClass: KClass<T>, predicate: (T) -> Boolean): T?
    public suspend fun removePermissions(predicate: ((Permission) -> Boolean)? = null)

    // -- SDK --

    public suspend fun addMigrations(migrations: Set<String>)

    override fun extend(beaconConfiguration: BeaconConfiguration): ExtendedStorage = this
}

public suspend inline fun <reified T : Peer> ExtendedStorage.findPeer(noinline predicate: (T) -> Boolean): T? = findPeer(T::class, predicate)
public suspend inline fun <reified T : AppMetadata> ExtendedStorage.findAppMetadata(noinline predicate: (T) -> Boolean): T? = findAppMetadata(T::class, predicate)
public suspend inline fun <reified T : Permission> ExtendedStorage.findPermission(noinline predicate: (T) -> Boolean): T? = findPermission(T::class, predicate)