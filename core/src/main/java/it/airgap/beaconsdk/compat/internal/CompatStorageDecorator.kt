package it.airgap.beaconsdk.compat.internal

import it.airgap.beaconsdk.data.account.AccountInfo
import it.airgap.beaconsdk.data.p2p.P2pPeerInfo
import it.airgap.beaconsdk.data.permission.PermissionInfo
import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.compat.storage.BeaconCompatStorage
import it.airgap.beaconsdk.storage.BeaconStorage
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class CompatStorageDecorator(private val storage: BeaconCompatStorage) : BeaconStorage {
    override suspend fun getP2pPeers(): List<P2pPeerInfo> = get(BeaconCompatStorage::getP2pPeers)
    override suspend fun setP2pPeers(p2pPeers: List<P2pPeerInfo>) = set(p2pPeers, BeaconCompatStorage::setP2pPeers)

    override suspend fun getAccounts(): List<AccountInfo> = get(BeaconCompatStorage::getAccounts)
    override suspend fun setAccounts(accounts: List<AccountInfo>) = set(accounts, BeaconCompatStorage::setAccounts)

    override suspend fun getActiveAccountIdentifier(): String? = get(BeaconCompatStorage::getActiveAccountIdentifier)
    override suspend fun setActiveAccountIdentifier(activeAccountIdentifier: String) = set(activeAccountIdentifier, BeaconCompatStorage::setActiveAccountIdentifier)

    override suspend fun getAppsMetadata(): List<AppMetadata> = get(BeaconCompatStorage::getAppsMetadata)
    override suspend fun setAppsMetadata(appsMetadata: List<AppMetadata>) = set(appsMetadata, BeaconCompatStorage::setAppsMetadata)

    override suspend fun getPermissions(): List<PermissionInfo> = get(BeaconCompatStorage::getPermissions)
    override suspend fun setPermissions(permissions: List<PermissionInfo>) = set(permissions, BeaconCompatStorage::setPermissions)

    override suspend fun getSdkSecretSeed(): String? = get(BeaconCompatStorage::getSdkSecretSeed)
    override suspend fun setSdkSecretSeed(sdkSecretSeed: String) = set(sdkSecretSeed, BeaconCompatStorage::setSdkSecretSeed)

    override suspend fun getSdkVersion(): String? = get(BeaconCompatStorage::getSdkVersion)
    override suspend fun setSdkVersion(sdkVersion: String) = set(sdkVersion, BeaconCompatStorage::setSdkVersion)

    private suspend fun <T> get(getter: BeaconCompatStorage.(BeaconCompatStorage.OnReadListener<T>) -> Unit) =
        suspendCoroutine<T> {
            getter(storage, object : BeaconCompatStorage.OnReadListener<T> {
                override fun onSuccess(value: T) {
                    it.resume(value)
                }

                override fun onError(error: Throwable) {
                    it.resumeWithException(error)
                }

            })
        }

    private suspend fun <T> set(value: T, setter: BeaconCompatStorage.(T, BeaconCompatStorage.OnWriteListener) -> Unit) =
        suspendCoroutine<Unit> {
            setter(storage, value, object : BeaconCompatStorage.OnWriteListener {
                override fun onSuccess() {
                    it.resume(Unit)
                }

                override fun onError(error: Throwable) {
                    it.resumeWithException(error)
                }

            })
        }
}