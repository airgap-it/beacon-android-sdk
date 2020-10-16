package it.airgap.beaconsdk.compat.storage

import it.airgap.beaconsdk.data.account.AccountInfo
import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.data.p2p.P2pPairingRequest
import it.airgap.beaconsdk.data.permission.PermissionInfo

interface BeaconCompatStorage {
    fun getP2pPeers(listener: OnReadListener<List<P2pPairingRequest>>)
    fun setP2pPeers(p2pPeers: List<P2pPairingRequest>, listener: OnWriteListener)

    fun getAccounts(listener: OnReadListener<List<AccountInfo>>)
    fun setAccounts(accounts: List<AccountInfo>, listener: OnWriteListener)

    fun getActiveAccountIdentifier(listener: OnReadListener<String?>)
    fun setActiveAccountIdentifier(activeAccountIdentifier: String, listener: OnWriteListener)

    fun getAppsMetadata(listener: OnReadListener<List<AppMetadata>>)
    fun setAppsMetadata(appsMetadata: List<AppMetadata>, listener: OnWriteListener)

    fun getPermissions(listener: OnReadListener<List<PermissionInfo>>)
    fun setPermissions(permissions: List<PermissionInfo>, listener: OnWriteListener)

    fun getSdkSecretSeed(listener: OnReadListener<String?>)
    fun setSdkSecretSeed(sdkSecretSeed: String, listener: OnWriteListener)

    fun getSdkVersion(listener: OnReadListener<String?>)
    fun setSdkVersion(sdkVersion: String, listener: OnWriteListener)

    companion object

    interface OnReadListener<T> {
        fun onSuccess(value: T)
        fun onError(error: Throwable)
    }

    interface OnWriteListener {
        fun onSuccess()
        fun onError(error: Throwable)
    }
}