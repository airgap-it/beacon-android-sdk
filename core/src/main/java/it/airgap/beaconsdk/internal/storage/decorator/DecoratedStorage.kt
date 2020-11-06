package it.airgap.beaconsdk.internal.storage.decorator

import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.P2pPeerInfo
import it.airgap.beaconsdk.data.beacon.PermissionInfo
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

    private val _permissions: MutableSharedFlow<PermissionInfo> by lazy { resourceFlow() }
    override val permissions: Flow<PermissionInfo> get() = _permissions.onSubscription { emitAll(getPermissions().asFlow()) }

    private val _p2pPeers: MutableSharedFlow<P2pPeerInfo> by lazy { resourceFlow() }
    override val p2pPeers: Flow<P2pPeerInfo> get() = _p2pPeers.onSubscription { emitAll(getP2pPeers().asFlow()) }

    override suspend fun addP2pPeers(
        peers: List<P2pPeerInfo>,
        overwrite: Boolean,
        compare: (P2pPeerInfo, P2pPeerInfo) -> Boolean,
    ) {
        add(
            Storage::getP2pPeers,
            Storage::setP2pPeers,
            _p2pPeers,
            peers,
            overwrite,
            compare,
        )
    }

    override suspend fun findP2pPeer(predicate: (P2pPeerInfo) -> Boolean): P2pPeerInfo? =
        selectFirst(Storage::getP2pPeers, predicate)

    override suspend fun removeP2pPeers(predicate: ((P2pPeerInfo) -> Boolean)?) {
        if (predicate != null) remove(Storage::getP2pPeers, Storage::setP2pPeers, predicate)
        else removeAll(Storage::setP2pPeers)
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
        permissions: List<PermissionInfo>,
        overwrite: Boolean,
        compare: (PermissionInfo, PermissionInfo) -> Boolean,
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

    override suspend fun findPermission(predicate: (PermissionInfo) -> Boolean): PermissionInfo? =
        selectFirst(Storage::getPermissions, predicate)

    override suspend fun removePermissions(predicate: ((PermissionInfo) -> Boolean)?) {
        if (predicate != null) remove(Storage::getPermissions, Storage::setPermissions, predicate)
        else removeAll(Storage::setPermissions)
    }

    override fun extend(): ExtendedStorage = this

    private suspend fun <T> selectFirst(
        select: StorageSelectCollection<T>,
        predicate: (T) -> Boolean,
    ): T? = select(this).find(predicate)

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
            !entities.map {
                compare(
                    toInsert,
                    it
                )
            }.fold(false, Boolean::or)
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