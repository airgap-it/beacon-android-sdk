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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DecoratedStorage(
    private val storage: Storage,
    private val beaconConfiguration: BeaconConfiguration,
) : ExtendedStorage, Storage by storage {
    override val peers: Flow<Peer>
        get() = with(ResourceCollection.PeerList) {
            resourceFlow.onSubscription { emitAll(getPeers(beaconConfiguration).asFlow()) }
        }

    override val appMetadata: Flow<AppMetadata>
        get() = with(ResourceCollection.AppMetadataList) {
            resourceFlow.onSubscription { emitAll(getAppMetadata(beaconConfiguration).asFlow()) }
        }

    override val permissions: Flow<Permission>
        get() = with(ResourceCollection.PermissionList) {
            resourceFlow.onSubscription { emitAll(getPermissions(beaconConfiguration).asFlow()) }
        }

    override suspend fun addPeers(
        peers: List<Peer>,
        overwrite: Boolean,
        selector: Peer.() -> List<Any>?,
    ) {
        add(ResourceCollection.PeerList, peers, overwrite, selector)
    }

    override suspend fun findPeer(predicate: (Peer) -> Boolean): Peer? =
        selectFirst(ResourceCollection.PeerList, predicate)

    override suspend fun <T : Peer> findPeer(
        instanceClass: KClass<T>,
        predicate: (T) -> Boolean,
    ): T? = selectFirstInstance(ResourceCollection.PeerList, instanceClass, predicate)

    override suspend fun removePeers(predicate: ((Peer) -> Boolean)?) {
        if (predicate != null) remove(ResourceCollection.PeerList, predicate)
        else removeAll(ResourceCollection.PeerList)
    }

    override suspend fun addAppMetadata(
        appsMetadata: List<AppMetadata>,
        overwrite: Boolean,
        selector: AppMetadata.() -> List<Any>?,
    ) {
        add(ResourceCollection.AppMetadataList, appsMetadata, overwrite, selector)
    }

    override suspend fun findAppMetadata(predicate: (AppMetadata) -> Boolean): AppMetadata? =
        selectFirst(ResourceCollection.AppMetadataList, predicate)

    override suspend fun <T : AppMetadata> findAppMetadata(
        instanceClass: KClass<T>,
        predicate: (T) -> Boolean,
    ): T? = selectFirstInstance(ResourceCollection.AppMetadataList, instanceClass, predicate)

    override suspend fun removeAppMetadata(predicate: ((AppMetadata) -> Boolean)?) {
        if (predicate != null) remove(ResourceCollection.AppMetadataList, predicate)
        else removeAll(ResourceCollection.AppMetadataList)
    }

    override suspend fun addPermissions(
        permissions: List<Permission>,
        overwrite: Boolean,
        selector: Permission.() -> List<Any>?,
    ) {
        add(ResourceCollection.PermissionList, permissions, overwrite, selector)
    }

    override suspend fun findPermission(predicate: (Permission) -> Boolean): Permission? =
        selectFirst(ResourceCollection.PermissionList, predicate)

    override suspend fun <T : Permission> findPermission(
        instanceClass: KClass<T>,
        predicate: (T) -> Boolean,
    ): T? = selectFirstInstance(ResourceCollection.PermissionList, instanceClass, predicate)

    override suspend fun removePermissions(predicate: ((Permission) -> Boolean)?) {
        if (predicate != null) remove(ResourceCollection.PermissionList, predicate)
        else removeAll(ResourceCollection.PermissionList)
    }

    override suspend fun addMigrations(migrations: Set<String>) {
        ResourceCollection.MigrationSet.runAtomic {
            val storageMigrations = getMigrations()
                .toMutableSet()
                .also { it.addAll(migrations) }

            setMigrations(storageMigrations)
        }
    }

    override fun scoped(beaconScope: BeaconScope): ExtendedStorage = DecoratedStorage(storage.scoped(beaconScope), beaconConfiguration)
    override fun extend(beaconConfiguration: BeaconConfiguration): ExtendedStorage = this

    private suspend fun <T, C : Collection<T>> selectFirst(
        resourceCollection: ResourceCollection<T, C>,
        predicate: (T) -> Boolean,
    ): T? = resourceCollection.runAtomic { select(beaconConfiguration).find(predicate) }

    private suspend fun <T : Any, C : Collection<T>, Instance : T> selectFirstInstance(
        resourceCollection: ResourceCollection<T, C>,
        instanceClass: KClass<Instance>,
        predicate: (Instance) -> Boolean,
    ): Instance? = resourceCollection.runAtomic { select(beaconConfiguration).filterIsInstance(instanceClass.java).find(predicate) }

    private suspend fun <T> add(
        resourceCollection: ResourceCollection<T, List<T>>,
        elements: List<T>,
        overwrite: Boolean,
        selector: T.() -> List<Any>?,
    ) {
        resourceCollection.runAtomic {
            val stored = select(beaconConfiguration).distinctByKeepLast(selector)

            val mappedIndices = createMappedIndices(stored, elements, selector)
            val (toInsert, updatedIndices) = stored.updatedWith(elements, mappedIndices, overwrite)

            insert(toInsert, beaconConfiguration)

            CoroutineScope(Dispatchers.Default).launch {
                toInsert.filterIndexed { index, _ -> updatedIndices.contains(index) }.forEach { resourceFlow.emit(it) }
            }
        }
    }

    private suspend fun <T> remove(resourceCollection: ResourceCollection<T, List<T>>, predicate: (T) -> Boolean) {
        resourceCollection.runAtomic { insert(select(beaconConfiguration).filterNot(predicate), beaconConfiguration) }
    }

    private suspend fun <T> removeAll(resourceCollection: ResourceCollection<T, List<T>>) {
        resourceCollection.runAtomic { insert(emptyList(), beaconConfiguration) }
    }

    private fun <T> createMappedIndices(first: List<T>, second: List<T>, selector: T.() -> List<Any>?): Map<Int, Int> {
        val indices = mutableMapOf<Int, List<Int>>().apply {
            fillWith(first, selector)
            fillWith(second, selector)
        }.toMap()

        return indices.values.filter { it.size == 2 }.associate { it[0] to it[1] }
    }

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

    private sealed class ResourceCollection<T, C : Collection<T>> {
        abstract val Storage.resourceFlow: MutableSharedFlow<T>

        abstract suspend fun Storage.select(beaconConfiguration: BeaconConfiguration): C
        abstract suspend fun Storage.insert(collection: C, beaconConfiguration: BeaconConfiguration)

        private val mutex: Mutex = Mutex()
        suspend inline fun <R> runAtomic(action: ResourceCollection<T, C>.() -> R): R =
            mutex.withLock { action(this) }

        protected fun <T> resourceFlow(bufferCapacity: Int = 64): MutableSharedFlow<T> =
            MutableSharedFlow(extraBufferCapacity = bufferCapacity)

        object PeerList : ResourceCollection<Peer, List<Peer>>() {
            override val Storage.resourceFlow: MutableSharedFlow<Peer> by lazy { resourceFlow() }

            override suspend fun Storage.select(beaconConfiguration: BeaconConfiguration): List<Peer> =
                getPeers(beaconConfiguration)

            override suspend fun Storage.insert(collection: List<Peer>, beaconConfiguration: BeaconConfiguration) {
                setPeers(collection)
            }
        }

        object AppMetadataList : ResourceCollection<AppMetadata, List<AppMetadata>>() {
            override val Storage.resourceFlow: MutableSharedFlow<AppMetadata> by lazy { resourceFlow() }

            override suspend fun Storage.select(beaconConfiguration: BeaconConfiguration): List<AppMetadata> =
                getAppMetadata(beaconConfiguration)

            override suspend fun Storage.insert(collection: List<AppMetadata>, beaconConfiguration: BeaconConfiguration) {
                setAppMetadata(collection)
            }
        }

        object PermissionList : ResourceCollection<Permission, List<Permission>>() {
            override val Storage.resourceFlow: MutableSharedFlow<Permission> by lazy { resourceFlow() }

            override suspend fun Storage.select(beaconConfiguration: BeaconConfiguration): List<Permission> =
                getPermissions(beaconConfiguration)

            override suspend fun Storage.insert(collection: List<Permission>, beaconConfiguration: BeaconConfiguration) {
                setPermissions(collection)
            }
        }

        object MigrationSet : ResourceCollection<String, Set<String>>() {
            override val Storage.resourceFlow: MutableSharedFlow<String> by lazy { resourceFlow() }

            override suspend fun Storage.select(beaconConfiguration: BeaconConfiguration): Set<String> =
                getMigrations()

            override suspend fun Storage.insert(collection: Set<String>, beaconConfiguration: BeaconConfiguration) {
                setMigrations(collection)
            }
        }
    }
}
