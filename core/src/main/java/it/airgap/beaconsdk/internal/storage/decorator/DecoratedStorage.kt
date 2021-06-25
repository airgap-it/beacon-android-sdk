package it.airgap.beaconsdk.internal.storage.decorator

import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.Peer
import it.airgap.beaconsdk.data.beacon.Permission
import it.airgap.beaconsdk.internal.storage.ExtendedStorage
import it.airgap.beaconsdk.internal.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private typealias StorageSelectCollection<T> = suspend Storage.() -> List<T>
private typealias StorageInsertCollection<T> = suspend Storage.(List<T>) -> Unit

internal class DecoratedStorage(private val storage: Storage) : ExtendedStorage, Storage by storage {
    private val _appMetadata: MutableSharedFlow<AppMetadata> by lazy { resourceFlow() }
    override val appMetadata: Flow<AppMetadata> get() = _appMetadata.onSubscription { emitAll(getAppMetadata().asFlow()) }

    private val _permissions: MutableSharedFlow<Permission> by lazy { resourceFlow() }
    override val permissions: Flow<Permission> get() = _permissions.onSubscription { emitAll(getPermissions().asFlow()) }

    private val _peers: MutableSharedFlow<Peer> by lazy { resourceFlow() }
    override val peers: Flow<Peer> get() = _peers.onSubscription { emitAll(getPeers().asFlow()) }

    override suspend fun addPeers(
        peers: List<Peer>,
        overwrite: Boolean,
        compare: (Peer, Peer) -> Boolean,
    ) {
        add(
            Storage::getPeers,
            Storage::setPeers,
            _peers,
            peers,
            overwrite,
            compare,
        )
    }

    override suspend fun findPeer(predicate: (Peer) -> Boolean): Peer? =
        selectFirst(Storage::getPeers, predicate)

    override suspend fun <T : Peer> findPeer(
        instanceClass: Class<T>,
        predicate: (T) -> Boolean,
    ): T? = selectFirstInstance(Storage::getPeers, instanceClass, predicate)

    override suspend fun removePeers(predicate: ((Peer) -> Boolean)?) {
        if (predicate != null) remove(Storage::getPeers, Storage::setPeers, predicate)
        else removeAll(Storage::setPeers)
    }

    override suspend fun addAppMetadata(
        appsMetadata: List<AppMetadata>,
        overwrite: Boolean,
        compare: (AppMetadata, AppMetadata) -> Boolean,
    ) {
        add(
            Storage::getAppMetadata,
            Storage::setAppMetadata,
            _appMetadata,
            appsMetadata,
            overwrite,
            compare,
        )
    }

    override suspend fun findAppMetadata(predicate: (AppMetadata) -> Boolean): AppMetadata? =
        selectFirst(Storage::getAppMetadata, predicate)

    override suspend fun removeAppMetadata(predicate: ((AppMetadata) -> Boolean)?) {
        if (predicate != null) remove(Storage::getAppMetadata, Storage::setAppMetadata, predicate)
        else removeAll(Storage::setAppMetadata)
    }

    override suspend fun addPermissions(
        permissions: List<Permission>,
        overwrite: Boolean,
        compare: (Permission, Permission) -> Boolean,
    ) {
        add(
            Storage::getPermissions,
            Storage::setPermissions,
            _permissions,
            permissions,
            overwrite,
            compare,
        )
    }

    override suspend fun findPermission(predicate: (Permission) -> Boolean): Permission? =
        selectFirst(Storage::getPermissions, predicate)

    override suspend fun removePermissions(predicate: ((Permission) -> Boolean)?) {
        if (predicate != null) remove(Storage::getPermissions, Storage::setPermissions, predicate)
        else removeAll(Storage::setPermissions)
    }

    override fun extend(): ExtendedStorage = this

    private suspend fun <T> selectFirst(
        select: StorageSelectCollection<T>,
        predicate: (T) -> Boolean,
    ): T? = select(this).find(predicate)

    private suspend fun <T, Instance : T> selectFirstInstance(
        select: StorageSelectCollection<T>,
        instanceClass: Class<Instance>,
        predicate: (Instance) -> Boolean,
    ): Instance? = select(this).filterIsInstance(instanceClass).find(predicate)

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