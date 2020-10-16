package it.airgap.beaconsdk.storage

import it.airgap.beaconsdk.data.account.AccountInfo
import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.data.p2p.P2pPairingRequest
import it.airgap.beaconsdk.data.permission.PermissionInfo

internal class MockBeaconStorage : BeaconStorage {
    private var p2pPeers: List<P2pPairingRequest> = emptyList()
    private var accounts: List<AccountInfo> = emptyList()
    private var activeAccountIdentifier: String? = null
    private var appsMetadata: List<AppMetadata> = emptyList()
    private var permissions: List<PermissionInfo> = emptyList()
    private var sdkSecretSeed: String? = null
    private var sdkVersion: String? = null

    override fun getP2pPeers(listener: BeaconStorage.OnReadListener<List<P2pPairingRequest>>) {
        listener.onSuccess(p2pPeers)
    }

    override fun setP2pPeers(p2pPeers: List<P2pPairingRequest>, listener: BeaconStorage.OnWriteListener) {
        this.p2pPeers = p2pPeers
        listener.onSuccess()
    }

    override fun getAccounts(listener: BeaconStorage.OnReadListener<List<AccountInfo>>) {
        listener.onSuccess(accounts)
    }

    override fun setAccounts(accounts: List<AccountInfo>, listener: BeaconStorage.OnWriteListener) {
        this.accounts = accounts
        listener.onSuccess()
    }

    override fun getActiveAccountIdentifier(listener: BeaconStorage.OnReadListener<String?>) {
        listener.onSuccess(activeAccountIdentifier)
    }

    override fun setActiveAccountIdentifier(activeAccountIdentifier: String, listener: BeaconStorage.OnWriteListener) {
        this.activeAccountIdentifier = activeAccountIdentifier
        listener.onSuccess()
    }

    override fun getAppsMetadata(listener: BeaconStorage.OnReadListener<List<AppMetadata>>) {
        listener.onSuccess(appsMetadata)
    }

    override fun setAppsMetadata(appsMetadata: List<AppMetadata>, listener: BeaconStorage.OnWriteListener) {
        this.appsMetadata = appsMetadata
        listener.onSuccess()
    }

    override fun getPermissions(listener: BeaconStorage.OnReadListener<List<PermissionInfo>>) {
        listener.onSuccess(permissions)
    }

    override fun setPermissions(permissions: List<PermissionInfo>, listener: BeaconStorage.OnWriteListener) {
        this.permissions = permissions
        listener.onSuccess()
    }

    override fun getSdkSecretSeed(listener: BeaconStorage.OnReadListener<String?>) {
        listener.onSuccess(sdkSecretSeed)
    }

    override fun setSdkSecretSeed(sdkSecretSeed: String, listener: BeaconStorage.OnWriteListener) {
        this.sdkSecretSeed = sdkSecretSeed
        listener.onSuccess()
    }

    override fun getSdkVersion(listener: BeaconStorage.OnReadListener<String?>) {
        listener.onSuccess(sdkVersion)
    }

    override fun setSdkVersion(sdkVersion: String, listener: BeaconStorage.OnWriteListener) {
        this.sdkVersion = sdkVersion
        listener.onSuccess()
    }
}