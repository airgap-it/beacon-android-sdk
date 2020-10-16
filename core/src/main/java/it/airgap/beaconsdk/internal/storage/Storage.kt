package it.airgap.beaconsdk.internal.storage

import it.airgap.beaconsdk.data.account.AccountInfo
import it.airgap.beaconsdk.data.p2p.P2pPairingRequest
import it.airgap.beaconsdk.data.permission.PermissionInfo
import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.storage.BeaconStorageKtx
import it.airgap.beaconsdk.storage.BeaconStorage
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal interface Storage {
    suspend fun getP2pPeers(): List<P2pPairingRequest>
    suspend fun setP2pPeers(p2pPeers: List<P2pPairingRequest>)

    suspend fun getAccounts(): List<AccountInfo>
    suspend fun setAccounts(accounts: List<AccountInfo>)

    suspend fun getActiveAccountIdentifier(): String?
    suspend fun setActiveAccountIdentifier(activeAccountIdentifier: String)

    suspend fun getAppsMetadata(): List<AppMetadata>
    suspend fun setAppsMetadata(appsMetadata: List<AppMetadata>)

    suspend fun getPermissions(): List<PermissionInfo>
    suspend fun setPermissions(permissions: List<PermissionInfo>)

    suspend fun getSdkSecretSeed(): String?
    suspend fun setSdkSecretSeed(sdkSecretSeed: String)

    suspend fun getSdkVersion(): String?
    suspend fun setSdkVersion(sdkVersion: String)

    class CallbackDecorator(private val callbackStorage: BeaconStorage) : Storage {
        override suspend fun getP2pPeers(): List<P2pPairingRequest> = get(BeaconStorage::getP2pPeers)
        override suspend fun setP2pPeers(p2pPeers: List<P2pPairingRequest>) = set(p2pPeers, BeaconStorage::setP2pPeers)

        override suspend fun getAccounts(): List<AccountInfo> = get(BeaconStorage::getAccounts)
        override suspend fun setAccounts(accounts: List<AccountInfo>) = set(accounts, BeaconStorage::setAccounts)

        override suspend fun getActiveAccountIdentifier(): String? = get(BeaconStorage::getActiveAccountIdentifier)
        override suspend fun setActiveAccountIdentifier(activeAccountIdentifier: String) = set(activeAccountIdentifier, BeaconStorage::setActiveAccountIdentifier)

        override suspend fun getAppsMetadata(): List<AppMetadata> = get(BeaconStorage::getAppsMetadata)
        override suspend fun setAppsMetadata(appsMetadata: List<AppMetadata>) = set(appsMetadata, BeaconStorage::setAppsMetadata)

        override suspend fun getPermissions(): List<PermissionInfo> = get(BeaconStorage::getPermissions)
        override suspend fun setPermissions(permissions: List<PermissionInfo>) = set(permissions, BeaconStorage::setPermissions)

        override suspend fun getSdkSecretSeed(): String? = get(BeaconStorage::getSdkSecretSeed)
        override suspend fun setSdkSecretSeed(sdkSecretSeed: String) = set(sdkSecretSeed, BeaconStorage::setSdkSecretSeed)

        override suspend fun getSdkVersion(): String? = get(BeaconStorage::getSdkVersion)
        override suspend fun setSdkVersion(sdkVersion: String) = set(sdkVersion, BeaconStorage::setSdkVersion)

        private suspend fun <T> get(getter: BeaconStorage.(BeaconStorage.OnReadListener<T>) -> Unit) =
            suspendCoroutine<T> {
                getter(callbackStorage, object : BeaconStorage.OnReadListener<T> {
                    override fun onSuccess(value: T) {
                        it.resume(value)
                    }

                    override fun onError(error: Throwable) {
                        it.resumeWithException(error)
                    }

                })
            }

        private suspend fun <T> set(value: T, setter: BeaconStorage.(T, BeaconStorage.OnWriteListener) -> Unit) =
            suspendCoroutine<Unit> {
                setter(callbackStorage, value, object : BeaconStorage.OnWriteListener {
                    override fun onSuccess() {
                        it.resume(Unit)
                    }

                    override fun onError(error: Throwable) {
                        it.resumeWithException(error)
                    }

                })
            }
    }

    class KtxDecorator(private val ktxStorage: BeaconStorageKtx) : Storage {
        override suspend fun getP2pPeers(): List<P2pPairingRequest> = ktxStorage.getP2pPeers()
        override suspend fun setP2pPeers(p2pPeers: List<P2pPairingRequest>) = ktxStorage.setP2pPeers(p2pPeers)

        override suspend fun getAccounts(): List<AccountInfo> = ktxStorage.getAccounts()
        override suspend fun setAccounts(accounts: List<AccountInfo>) = ktxStorage.setAccounts(accounts)

        override suspend fun getActiveAccountIdentifier(): String? = ktxStorage.getActiveAccountIdentifier()
        override suspend fun setActiveAccountIdentifier(activeAccountIdentifier: String) = ktxStorage.setActiveAccountIdentifier(activeAccountIdentifier)

        override suspend fun getAppsMetadata(): List<AppMetadata> = ktxStorage.getAppsMetadata()
        override suspend fun setAppsMetadata(appsMetadata: List<AppMetadata>) = ktxStorage.setAppsMetadata(appsMetadata)

        override suspend fun getPermissions(): List<PermissionInfo> = ktxStorage.getPermissions()
        override suspend fun setPermissions(permissions: List<PermissionInfo>) = ktxStorage.setPermissions(permissions)

        override suspend fun getSdkSecretSeed(): String? = ktxStorage.getSdkSecretSeed()
        override suspend fun setSdkSecretSeed(sdkSecretSeed: String) = ktxStorage.setSdkSecretSeed(sdkSecretSeed)

        override suspend fun getSdkVersion(): String? = ktxStorage.getSdkVersion()
        override suspend fun setSdkVersion(sdkVersion: String) = ktxStorage.setSdkVersion(sdkVersion)
    }
}