package it.airgap.beaconsdk.core.internal.storage.decorator

import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.Peer
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.getAppMetadata
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.getPeers
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.getPermissions
import it.airgap.beaconsdk.core.storage.ExtendedStorage
import it.airgap.beaconsdk.core.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

private typealias StorageSelectCollection<T> = suspend Storage.() -> List<T>
private typealias StorageInsertCollection<T> = suspend Storage.(List<T>) -> Unit

internal class DecoratedStorage(
    private val storage: Storage,
    private val configuration: BeaconConfiguration,
) : ExtendedStorage, Storage by storage {
    private val _appMetadata: MutableSharedFlow<AppMetadata> by lazy { resourceFlow() }
    override val appMetadata: Flow<AppMetadata> get() = _appMetadata.onSubscription { emitAll(getAppMetadata(configuration).asFlow()) }

    private val _permissions: MutableSharedFlow<Permission> by lazy { resourceFlow() }
    override val permissions: Flow<Permission> get() = _permissions.onSubscription { emitAll(getPermissions(configuration).asFlow()) }

    private val _peers: MutableSharedFlow<Peer> by lazy { resourceFlow() }
    override val peers: Flow<Peer> get() = _peers.onSubscription { emitAll(getPeers(configuration).asFlow()) }

    override suspend fun addPeers(
        peers: List<Peer>,
        overwrite: Boolean,
        compare: (Peer, Peer) -> Boolean,
    ) {
        add(
            { getPeers(configuration) },
            Storage::setPeers,
            _peers,
            peers,
            overwrite,
            compare,
        )
    }

    override suspend fun findPeer(predicate: (Peer) -> Boolean): Peer? =
        selectFirst({ getPeers(configuration) }, predicate)

    override suspend fun <T : Peer> findPeer(
        instanceClass: KClass<T>,
        predicate: (T) -> Boolean,
    ): T? = selectFirstInstance({ getPeers(configuration) }, instanceClass, predicate)

    override suspend fun removePeers(predicate: ((Peer) -> Boolean)?) {
        if (predicate != null) remove({ getPeers(configuration) }, Storage::setPeers, predicate)
        else removeAll(Storage::setPeers)
    }

    override suspend fun addAppMetadata(
        appsMetadata: List<AppMetadata>,
        overwrite: Boolean,
        compare: (AppMetadata, AppMetadata) -> Boolean,
    ) {
        add(
            { getAppMetadata(configuration) },
            Storage::setAppMetadata,
            _appMetadata,
            appsMetadata,
            overwrite,
            compare,
        )
    }

    override suspend fun findAppMetadata(predicate: (AppMetadata) -> Boolean): AppMetadata? =
        selectFirst({ getAppMetadata(configuration) }, predicate)

    override suspend fun <T : AppMetadata> findAppMetadata(
        instanceClass: KClass<T>,
        predicate: (T) -> Boolean,
    ): T? = selectFirstInstance({ getAppMetadata(configuration) }, instanceClass, predicate)

    override suspend fun removeAppMetadata(predicate: ((AppMetadata) -> Boolean)?) {
        if (predicate != null) remove({ getAppMetadata(configuration) }, Storage::setAppMetadata, predicate)
        else removeAll(Storage::setAppMetadata)
    }

    override suspend fun addPermissions(
        permissions: List<Permission>,
        overwrite: Boolean,
        compare: (Permission, Permission) -> Boolean,
    ) {
        add(
            { getPermissions(configuration) },
            Storage::setPermissions,
            _permissions,
            permissions,
            overwrite,
            compare,
        )
    }

    override suspend fun findPermission(predicate: (Permission) -> Boolean): Permission? =
        selectFirst({ getPermissions(configuration) }, predicate)

    override suspend fun <T : Permission> findPermission(
        instanceClass: KClass<T>,
        predicate: (T) -> Boolean,
    ): T? = selectFirstInstance({ getPermissions(configuration) }, instanceClass, predicate)

    override suspend fun removePermissions(predicate: ((Permission) -> Boolean)?) {
        if (predicate != null) remove({ getPermissions(configuration) }, Storage::setPermissions, predicate)
        else removeAll(Storage::setPermissions)
    }

    override suspend fun addMigrations(migrations: Set<String>) {
        val storageMigrations = getMigrations()
            .toMutableSet()
            .also { it.addAll(migrations) }

        setMigrations(storageMigrations)
    }

    override fun extend(beaconConfiguration: BeaconConfiguration): ExtendedStorage = this

    private suspend fun <T> selectFirst(
        select: StorageSelectCollection<T>,
        predicate: (T) -> Boolean,
    ): T? = select(this).find(predicate)

    private suspend fun <T : Any, Instance : T> selectFirstInstance(
        select: StorageSelectCollection<T>,
        instanceClass: KClass<Instance>,
        predicate: (Instance) -> Boolean,
    ): Instance? = select(this).filterIsInstance(instanceClass.java).find(predicate)

    private suspend fun <T> add(
        select: StorageSelectCollection<T>,
        insert: StorageInsertCollection<T>,
        subscribeFlow: MutableSharedFlow<T>,
        elements: List<T>,
        overwrite: Boolean,
        compare: (T, T) -> Boolean,
    ) {
        val entities = select(this).toMutableList()
        val updatedEntities = mutableListOf<T>()

        val (newEntities, existingElements) = elements.partition { toInsert ->
            !entities.any { compare(toInsert, it) }
        }

        if (overwrite) {
            existingElements
                .map { toInsert -> entities.indexOfFirst { compare(toInsert, it) } to toInsert }
                .forEach { (index, toInsert) ->
                    entities[index] = toInsert
                    updatedEntities.add(toInsert)
                }
        }

        insert(this, entities + newEntities)

        CoroutineScope(Dispatchers.Default).launch {
            (updatedEntities + newEntities).forEach { subscribeFlow.tryEmit(it) }
        }
    }

    private suspend fun <T> remove(
        select: StorageSelectCollection<T>,
        insert: StorageInsertCollection<T>,
        predicate: (T) -> Boolean,
    ) {
        insert(this, select(this).filterNot(predicate))
    }

    private suspend fun <T> removeAll(insert: StorageInsertCollection<T>) {
        insert(this, emptyList())
    }

    private fun <T> resourceFlow(bufferCapacity: Int = 64): MutableSharedFlow<T> =
        MutableSharedFlow(extraBufferCapacity = bufferCapacity)
}