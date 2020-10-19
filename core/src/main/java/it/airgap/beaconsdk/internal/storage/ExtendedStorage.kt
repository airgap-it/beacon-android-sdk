package it.airgap.beaconsdk.internal.storage

import it.airgap.beaconsdk.data.account.AccountInfo
import it.airgap.beaconsdk.data.p2p.P2pPeerInfo
import it.airgap.beaconsdk.data.permission.PermissionInfo
import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.storage.BeaconStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private typealias StorageSelectCollection<T> = suspend BeaconStorage.() -> List<T>
private typealias StorageInsertCollection<T> = suspend BeaconStorage.(List<T>) -> Unit

//@ExperimentalCoroutinesApi
internal class ExtendedStorage(private val storage: BeaconStorage) : BeaconStorage by storage {
    private val _accounts: MutableSharedFlow<AccountInfo> by lazy { resourceFlow() }
    val accounts: Flow<AccountInfo>
        get() = _accounts.onSubscription { emitAll(getAccounts().asFlow()) }

    private val _activeAccountIdentifier: MutableSharedFlow<String?> by lazy {
        resourceFlow(bufferCapacity = 1)
    }
    val activeAccountIdentifier: Flow<String?>
        get() = _activeAccountIdentifier.onSubscription { emit(getActiveAccountIdentifier()) }

    private val _appMetadata: MutableSharedFlow<AppMetadata> by lazy { resourceFlow() }
    val appMetadata: Flow<AppMetadata>
        get() = _appMetadata.onSubscription { emitAll(getAppsMetadata().asFlow()) }

    private val _permissions: MutableSharedFlow<PermissionInfo> by lazy { resourceFlow() }
    val permissions: Flow<PermissionInfo>
        get() = _permissions.onSubscription { emitAll(getPermissions().asFlow()) }

    private val _p2pPeers: MutableSharedFlow<P2pPeerInfo> by lazy { resourceFlow() }
    val p2pPeers: Flow<P2pPeerInfo>
        get() = _p2pPeers.onSubscription { emitAll(getP2pPeers().asFlow()) }

    override suspend fun setActiveAccountIdentifier(activeAccountIdentifier: String) {
        storage.setActiveAccountIdentifier(activeAccountIdentifier)
        CoroutineScope(Dispatchers.Default).launch { _activeAccountIdentifier.tryEmit(activeAccountIdentifier) }
    }

    suspend fun findAccount(predicate: (AccountInfo) -> Boolean): AccountInfo? =
        selectFirst(BeaconStorage::getAccounts, predicate)

    suspend fun addAccounts(
        vararg accounts: AccountInfo,
        overwrite: Boolean = false,
        compare: (AccountInfo, AccountInfo) -> Boolean = { first, second -> first == second }
    ) {
        addAccounts(accounts.toList(), overwrite, compare)
    }

    suspend fun addAccounts(
        accounts: List<AccountInfo>,
        overwrite: Boolean = false,
        compare: (AccountInfo, AccountInfo) -> Boolean = { first, second -> first == second }
    ) {
        add(
            BeaconStorage::getAccounts,
            BeaconStorage::setAccounts,
            _accounts,
            accounts,
            overwrite,
            compare
        )
    }

    suspend fun removeAccounts(predicate: ((AccountInfo) -> Boolean)? = null) {
        if (predicate != null) remove(
            BeaconStorage::getAccounts,
            BeaconStorage::setAccounts,
            predicate
        )
        else removeAll(BeaconStorage::setAccounts)
    }

    suspend fun findAppMetadata(predicate: (AppMetadata) -> Boolean): AppMetadata? =
        selectFirst(BeaconStorage::getAppsMetadata, predicate)

    suspend fun addAppsMetadata(
        vararg appsMetadata: AppMetadata,
        overwrite: Boolean = false,
        compare: (AppMetadata, AppMetadata) -> Boolean = { first, second -> first == second }
    ) {
        addAppsMetadata(appsMetadata.toList(), overwrite, compare)
    }

    suspend fun addAppsMetadata(
        appsMetadata: List<AppMetadata>,
        overwrite: Boolean = false,
        compare: (AppMetadata, AppMetadata) -> Boolean = { first, second -> first == second }
    ) {
        add(
            BeaconStorage::getAppsMetadata,
            BeaconStorage::setAppsMetadata,
            _appMetadata,
            appsMetadata,
            overwrite,
            compare
        )
    }

    suspend fun removeAppsMetadata(predicate: ((AppMetadata) -> Boolean)? = null) {
        if (predicate != null) remove(
            BeaconStorage::getAppsMetadata,
            BeaconStorage::setAppsMetadata,
            predicate
        )
        else removeAll(BeaconStorage::setAppsMetadata)
    }

    suspend fun findPermission(predicate: (PermissionInfo) -> Boolean): PermissionInfo? =
        selectFirst(BeaconStorage::getPermissions, predicate)

    suspend fun addPermissions(
        vararg permissions: PermissionInfo,
        overwrite: Boolean = false,
        compare: (PermissionInfo, PermissionInfo) -> Boolean = { first, second -> first == second }
    ) {
        addPermissions(permissions.toList(), overwrite, compare)
    }

    suspend fun addPermissions(
        permissions: List<PermissionInfo>,
        overwrite: Boolean = false,
        compare: (PermissionInfo, PermissionInfo) -> Boolean = { first, second -> first == second }
    ) {
        add(
            BeaconStorage::getPermissions,
            BeaconStorage::setPermissions,
            _permissions,
            permissions,
            overwrite,
            compare
        )
    }

    suspend fun removePermissions(predicate: ((PermissionInfo) -> Boolean)? = null) {
        if (predicate != null) remove(
            BeaconStorage::getPermissions,
            BeaconStorage::setPermissions,
            predicate
        )
        else removeAll(BeaconStorage::setPermissions)
    }

    suspend fun findP2pPeer(predicate: (P2pPeerInfo) -> Boolean): P2pPeerInfo? =
        selectFirst(BeaconStorage::getP2pPeers, predicate)

    suspend fun addP2pPeers(
        vararg peers: P2pPeerInfo,
        overwrite: Boolean = false,
        compare: (P2pPeerInfo, P2pPeerInfo) -> Boolean = { first, second -> first == second }
    ) {
        addP2pPeers(peers.toList(), overwrite, compare)
    }

    suspend fun addP2pPeers(
        peers: List<P2pPeerInfo>,
        overwrite: Boolean = false,
        compare: (P2pPeerInfo, P2pPeerInfo) -> Boolean = { first, second -> first == second }
    ) {
        add(
            BeaconStorage::getP2pPeers,
            BeaconStorage::setP2pPeers,
            _p2pPeers,
            peers,
            overwrite,
            compare
        )
    }

    suspend fun removeP2pPeers(predicate: ((P2pPeerInfo) -> Boolean)? = null) {
        if (predicate != null) remove(
            BeaconStorage::getP2pPeers,
            BeaconStorage::setP2pPeers,
            predicate
        )
        else removeAll(BeaconStorage::setP2pPeers)
    }

    private suspend fun <T> selectFirst(
        select: StorageSelectCollection<T>,
        predicate: (T) -> Boolean
    ): T? = select(this).find(predicate)

    private suspend fun <T> add(
        select: StorageSelectCollection<T>,
        insert: StorageInsertCollection<T>,
        subscribeFlow: MutableSharedFlow<T>,
        elements: List<T>,
        overwrite: Boolean,
        compare: (T, T) -> Boolean
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

        CoroutineScope(Dispatchers.Default).launch {
            (updatedEntities + newEntities).forEach { subscribeFlow.tryEmit(it) }
        }

        insert(this, entities + newEntities)
    }

    private suspend fun <T> remove(
        select: StorageSelectCollection<T>,
        insert: StorageInsertCollection<T>,
        predicate: (T) -> Boolean
    ) {
        insert(this, select(this).filterNot(predicate))
    }

    private suspend fun <T> removeAll(insert: StorageInsertCollection<T>) {
        insert(this, emptyList())
    }

    private fun <T> resourceFlow(bufferCapacity: Int = 64): MutableSharedFlow<T> =
        MutableSharedFlow(extraBufferCapacity = bufferCapacity)
}