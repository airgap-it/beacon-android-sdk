package it.airgap.beaconsdk.internal.storage

import it.airgap.beaconsdk.data.account.AccountInfo
import it.airgap.beaconsdk.data.p2p.P2pPairingRequest
import it.airgap.beaconsdk.data.permission.PermissionInfo
import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.storage.BeaconStorage

private typealias StorageSelectCollection<T> = suspend BeaconStorage.() -> List<T>
private typealias StorageInsertCollection<T> = suspend BeaconStorage.(List<T>) -> Unit

internal class ExtendedStorage(storage: BeaconStorage) : BeaconStorage by storage {
    suspend fun findAccount(predicate: (AccountInfo) -> Boolean): AccountInfo? = selectFirst(BeaconStorage::getAccounts, predicate)
    suspend fun addAccounts(
        vararg accounts: AccountInfo,
        overwrite: Boolean = true,
        compare: (AccountInfo, AccountInfo) -> Boolean = { first, second -> first == second }
    ) {
        add(BeaconStorage::getAccounts, BeaconStorage::setAccounts, accounts.toList(), overwrite, compare)
    }
    suspend fun removeAccounts(predicate: ((AccountInfo) -> Boolean)? = null) {
        if (predicate != null) remove(BeaconStorage::getAccounts, BeaconStorage::setAccounts, predicate)
        else removeAll(BeaconStorage::setAccounts)
    }

    suspend fun findAppMetadata(predicate: (AppMetadata) -> Boolean): AppMetadata? = selectFirst(BeaconStorage::getAppsMetadata, predicate)
    suspend fun addAppsMetadata(
        vararg appsMetadata: AppMetadata,
        overwrite: Boolean = true,
        compare: (AppMetadata, AppMetadata) -> Boolean = { first, second -> first == second }
    ) {
        add(BeaconStorage::getAppsMetadata, BeaconStorage::setAppsMetadata, appsMetadata.toList(), overwrite, compare)
    }
    suspend fun removeAppsMetadata(predicate: ((AppMetadata) -> Boolean)? = null) {
        if (predicate != null) remove(BeaconStorage::getAppsMetadata, BeaconStorage::setAppsMetadata, predicate)
        else removeAll(BeaconStorage::setAppsMetadata)
    }

    suspend fun findPermission(predicate: (PermissionInfo) -> Boolean): PermissionInfo? = selectFirst(BeaconStorage::getPermissions, predicate)
    suspend fun addPermissions(
        vararg permissions: PermissionInfo,
        overwrite: Boolean = true,
        compare: (PermissionInfo, PermissionInfo) -> Boolean = { first, second -> first == second }
    ) {
        add(BeaconStorage::getPermissions, BeaconStorage::setPermissions, permissions.toList(), overwrite, compare)
    }
    suspend fun removePermissions(predicate: ((PermissionInfo) -> Boolean)? = null) {
        if (predicate != null) remove(BeaconStorage::getPermissions, BeaconStorage::setPermissions, predicate)
        else removeAll(BeaconStorage::setPermissions)
    }

    suspend fun findP2pPeer(predicate: (P2pPairingRequest) -> Boolean): P2pPairingRequest? = selectFirst(BeaconStorage::getP2pPeers, predicate)
    suspend fun addP2pPeers(
        vararg peers: P2pPairingRequest,
        overwrite: Boolean = true,
        compare: (P2pPairingRequest, P2pPairingRequest) -> Boolean = { first, second -> first == second }
    ) {
        add(BeaconStorage::getP2pPeers, BeaconStorage::setP2pPeers, peers.toList(), overwrite, compare)
    }
    suspend fun removeP2pPeers(predicate: ((P2pPairingRequest) -> Boolean)? = null) {
        if (predicate != null) remove(BeaconStorage::getP2pPeers, BeaconStorage::setP2pPeers, predicate)
        else removeAll(BeaconStorage::setP2pPeers)
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