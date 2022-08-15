package it.airgap.beaconsdk.core.internal.storage.decorator

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.Peer
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.getAppMetadata
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.getPeers
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.getPermissions
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.ExtendedStorage
import it.airgap.beaconsdk.core.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

private typealias StorageSelectCollection<T> = suspend Storage.() -> List<T>
private typealias StorageInsertCollection<T> = suspend Storage.(List<T>) -> Unit

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DecoratedStorage(
    private val storage: Storage,
    private val beaconConfiguration: BeaconConfiguration,
) : ExtendedStorage, Storage by storage {
    private val _appMetadata: MutableSharedFlow<AppMetadata> by lazy { resourceFlow() }
    override val appMetadata: Flow<AppMetadata> get() = _appMetadata.onSubscription { emitAll(getAppMetadata(beaconConfiguration).asFlow()) }

    private val _permissions: MutableSharedFlow<Permission> by lazy { resourceFlow() }
    override val permissions: Flow<Permission> get() = _permissions.onSubscription { emitAll(getPermissions(beaconConfiguration).asFlow()) }

    private val _peers: MutableSharedFlow<Peer> by lazy { resourceFlow() }
    override val peers: Flow<Peer> get() = _peers.onSubscription { emitAll(getPeers(beaconConfiguration).asFlow()) }

    override suspend fun addPeers(
        peers: List<Peer>,
        overwrite: Boolean,
        selector: Peer.() -> List<Any>?,
    ) {
        add(
            { getPeers(beaconConfiguration) },
            Storage::setPeers,
            _peers,
            peers,
            overwrite,
            selector,
        )
    }

    override suspend fun findPeer(predicate: (Peer) -> Boolean): Peer? =
        selectFirst({ getPeers(beaconConfiguration) }, predicate)

    override suspend fun <T : Peer> findPeer(
        instanceClass: KClass<T>,
        predicate: (T) -> Boolean,
    ): T? = selectFirstInstance({ getPeers(beaconConfiguration) }, instanceClass, predicate)

    override suspend fun removePeers(predicate: ((Peer) -> Boolean)?) {
        if (predicate != null) remove({ getPeers(beaconConfiguration) }, Storage::setPeers, predicate)
        else removeAll(Storage::setPeers)
    }

    override suspend fun addAppMetadata(
        appsMetadata: List<AppMetadata>,
        overwrite: Boolean,
        selector: AppMetadata.() -> List<Any>?,
    ) {
        add(
            { getAppMetadata(beaconConfiguration) },
            Storage::setAppMetadata,
            _appMetadata,
            appsMetadata,
            overwrite,
            selector,
        )
    }

    override suspend fun findAppMetadata(predicate: (AppMetadata) -> Boolean): AppMetadata? =
        selectFirst({ getAppMetadata(beaconConfiguration) }, predicate)

    override suspend fun <T : AppMetadata> findAppMetadata(
        instanceClass: KClass<T>,
        predicate: (T) -> Boolean,
    ): T? = selectFirstInstance({ getAppMetadata(beaconConfiguration) }, instanceClass, predicate)

    override suspend fun removeAppMetadata(predicate: ((AppMetadata) -> Boolean)?) {
        if (predicate != null) remove({ getAppMetadata(beaconConfiguration) }, Storage::setAppMetadata, predicate)
        else removeAll(Storage::setAppMetadata)
    }

    override suspend fun addPermissions(
        permissions: List<Permission>,
        overwrite: Boolean,
        selector: Permission.() -> List<Any>?,
    ) {
        add(
            { getPermissions(beaconConfiguration) },
            Storage::setPermissions,
            _permissions,
            permissions,
            overwrite,
            selector,
        )
    }

    override suspend fun findPermission(predicate: (Permission) -> Boolean): Permission? =
        selectFirst({ getPermissions(beaconConfiguration) }, predicate)

    override suspend fun <T : Permission> findPermission(
        instanceClass: KClass<T>,
        predicate: (T) -> Boolean,
    ): T? = selectFirstInstance({ getPermissions(beaconConfiguration) }, instanceClass, predicate)

    override suspend fun removePermissions(predicate: ((Permission) -> Boolean)?) {
        if (predicate != null) remove({ getPermissions(beaconConfiguration) }, Storage::setPermissions, predicate)
        else removeAll(Storage::setPermissions)
    }

    override suspend fun addMigrations(migrations: Set<String>) {
        val storageMigrations = getMigrations()
            .toMutableSet()
            .also { it.addAll(migrations) }

        setMigrations(storageMigrations)
    }

    override fun scoped(beaconScope: BeaconScope): ExtendedStorage = DecoratedStorage(storage.scoped(beaconScope), beaconConfiguration)
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
        selector: T.() -> List<Any>?,
    ) {
        val stored = select(this).distinctByKeepLast(selector)

        val mappedIndices = createMappedIndices(stored, elements, selector)
        val (toInsert, updatedIndices) = stored.updatedWith(elements, mappedIndices, overwrite)

        insert(this, toInsert)

        CoroutineScope(Dispatchers.Default).launch {
            toInsert.filterIndexed { index, _ -> updatedIndices.contains(index) }.forEach { subscribeFlow.tryEmit(it) }
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

    private fun <T> createMappedIndices(first: List<T>, second: List<T>, selector: T.() -> List<Any>?): Map<Int, Int> {
        val indices = mutableMapOf<Int, List<Int>>().apply {
            fillWith(first, selector)
            fillWith(second, selector)
        }.toMap()

        return indices.values.filter { it.size == 2 }.associate { it[0] to it[1] }
    }

    private fun <T> resourceFlow(bufferCapacity: Int = 64): MutableSharedFlow<T> =
        MutableSharedFlow(extraBufferCapacity = bufferCapacity)

    private inline fun <T> List<T>.distinctByKeepLast(selector: T.() -> List<Any>?): List<T> {
        val (withSelector, withoutSelector) = map { selector(it)?.sumHashCodes() to it }
            .partition { it.first != null }
            .mapFirst { list -> list.associate { (it.first ?: it.second.hashCode()) to it.second } }

        return withSelector.values + withoutSelector.map { it.second }
    }

    private inline fun <T> MutableMap<Int, List<Int>>.fillWith(elements: List<T>, selector: T.() -> List<Any>?) {
        elements.forEachIndexed { index, element ->
            selector(element)?.sumHashCodes()?.let {
                this[it] = (this[it] ?: emptyList()) + index
            }
        }
    }

    private fun <T> List<T>.updatedWith(elements: List<T>, indicesMap: Map<Int, Int>, overwrite: Boolean): Pair<List<T>, Set<Int>> {
        val updated = toMutableList()
        val new = elements.filterIndexed { index, _ -> !indicesMap.containsValue(index) }

        val updatedIndices = mutableSetOf<Int>()
        val newIndices = (updated.size until updated.size + new.size)

        indices.forEach { index ->
            val mappedIndex = if (overwrite) indicesMap[index] else null
            mappedIndex?.let {
                updated[index] = elements[it]
                updatedIndices.add(index)
            }
        }

        return Pair(updated + new, updatedIndices + newIndices)
    }

    private fun List<Any>.sumHashCodes(): Int = fold(0) { acc, next -> acc + next.hashCode() }

    private fun <A, B, R> Pair<A, B>.mapFirst(transform: (A) -> R): Pair<R, B> = Pair(transform(first), second)
}