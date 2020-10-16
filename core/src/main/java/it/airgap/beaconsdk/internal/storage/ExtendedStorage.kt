package it.airgap.beaconsdk.internal.storage

import it.airgap.beaconsdk.data.account.AccountInfo
import it.airgap.beaconsdk.data.p2p.P2pPairingRequest
import it.airgap.beaconsdk.data.permission.PermissionInfo
import it.airgap.beaconsdk.data.sdk.AppMetadata

private typealias StorageSelectCollection<T> = suspend Storage.() -> List<T>
private typealias StorageInsertCollection<T> = suspend Storage.(List<T>) -> Unit

internal class ExtendedStorage(storage: Storage) : Storage by storage {
    suspend fun findAccount(predicate: (AccountInfo) -> Boolean): AccountInfo? = selectFirst(Storage::getAccounts, predicate)
    suspend fun addAccounts(
        vararg accounts: AccountInfo,
        overwrite: Boolean = true,
        compare: (AccountInfo, AccountInfo) -> Boolean = { first, second -> first == second }
    ) {
        add(Storage::getAccounts, Storage::setAccounts, accounts.toList(), overwrite, compare)
    }
    suspend fun removeAccounts(predicate: ((AccountInfo) -> Boolean)? = null) {
        if (predicate != null) remove(Storage::getAccounts, Storage::setAccounts, predicate)
        else removeAll(Storage::setAccounts)
    }

    suspend fun findAppMetadata(predicate: (AppMetadata) -> Boolean): AppMetadata? = selectFirst(Storage::getAppsMetadata, predicate)
    suspend fun addAppsMetadata(
        vararg appsMetadata: AppMetadata,
        overwrite: Boolean = true,
        compare: (AppMetadata, AppMetadata) -> Boolean = { first, second -> first == second }
    ) {
        add(Storage::getAppsMetadata, Storage::setAppsMetadata, appsMetadata.toList(), overwrite, compare)
    }
    suspend fun removeAppsMetadata(predicate: ((AppMetadata) -> Boolean)? = null) {
        if (predicate != null) remove(Storage::getAppsMetadata, Storage::setAppsMetadata, predicate)
        else removeAll(Storage::setAppsMetadata)
    }

    suspend fun findPermission(predicate: (PermissionInfo) -> Boolean): PermissionInfo? = selectFirst(Storage::getPermissions, predicate)
    suspend fun addPermissions(
        vararg permissions: PermissionInfo,
        overwrite: Boolean = true,
        compare: (PermissionInfo, PermissionInfo) -> Boolean = { first, second -> first == second }
    ) {
        add(Storage::getPermissions, Storage::setPermissions, permissions.toList(), overwrite, compare)
    }
    suspend fun removePermissions(predicate: ((PermissionInfo) -> Boolean)? = null) {
        if (predicate != null) remove(Storage::getPermissions, Storage::setPermissions, predicate)
        else removeAll(Storage::setPermissions)
    }

    suspend fun findP2pPeer(predicate: (P2pPairingRequest) -> Boolean): P2pPairingRequest? = selectFirst(Storage::getP2pPeers, predicate)
    suspend fun addP2pPeers(
        vararg peers: P2pPairingRequest,
        overwrite: Boolean = true,
        compare: (P2pPairingRequest, P2pPairingRequest) -> Boolean = { first, second -> first == second }
    ) {
        add(Storage::getP2pPeers, Storage::setP2pPeers, peers.toList(), overwrite, compare)
    }
    suspend fun removeP2pPeers(predicate: ((P2pPairingRequest) -> Boolean)? = null) {
        if (predicate != null) remove(Storage::getP2pPeers, Storage::setP2pPeers, predicate)
        else removeAll(Storage::setP2pPeers)
    }

    private suspend fun <T> selectFirst(select: StorageSelectCollection<T>, predicate: (T) -> Boolean): T? = select(this).find(predicate)
    private suspend fun <T> add(select: StorageSelectCollection<T>, insert: StorageInsertCollection<T>, elements: List<T>, overwrite: Boolean, compare: (T, T) -> Boolean) {
        val entities = select(this).toMutableList()

        val (newElements, existingElements) = elements.partition { toInsert -> !entities.map { compare(toInsert, it) }.fold(false, Boolean::or) }
        if (overwrite) {
            existingElements
                .map { toInsert -> entities.indexOfFirst { compare(toInsert, it) } to toInsert }
                .forEach { (index, toInsert) ->
                    entities[index] = toInsert
                }
        }

        insert(this, entities + newElements)
    }
    private suspend fun <T> remove(select: StorageSelectCollection<T>, insert: StorageInsertCollection<T>, predicate: (T) -> Boolean) {
        insert(this, select(this).filter(predicate))
    }
    private suspend fun <T> removeAll(insert: StorageInsertCollection<T>) {
        insert(this, emptyList())
    }
}