package it.airgap.beaconsdk.storage

import it.airgap.beaconsdk.data.account.AccountInfo
import it.airgap.beaconsdk.data.p2p.P2pPairingRequest
import it.airgap.beaconsdk.data.permission.PermissionInfo
import it.airgap.beaconsdk.data.sdk.AppMetadata

internal class MockBeaconStorage : BeaconStorage {
    private var p2pPeers: List<P2pPairingRequest> = emptyList()
    private var accounts: List<AccountInfo> = emptyList()
    private var activeAccountIdentifier: String? = null
    private var appsMetadata: List<AppMetadata> = emptyList()
    private var permissions: List<PermissionInfo> = emptyList()
    private var sdkSecretSeed: String? = null
    private var sdkVersion: String? = null

    override suspend fun getP2pPeers(): List<P2pPairingRequest> = p2pPeers
    override suspend fun setP2pPeers(p2pPeers: List<P2pPairingRequest>) {
        this.p2pPeers = p2pPeers
    }

    override suspend fun getAccounts(): List<AccountInfo> = accounts
    override suspend fun setAccounts(accounts: List<AccountInfo>) {
        this.accounts = accounts
    }

    override suspend fun getActiveAccountIdentifier(): String? = activeAccountIdentifier
    override suspend fun setActiveAccountIdentifier(activeAccountIdentifier: String) {
        this.activeAccountIdentifier = activeAccountIdentifier
    }

    override suspend fun getAppsMetadata(): List<AppMetadata> = appsMetadata
    override suspend fun setAppsMetadata(appsMetadata: List<AppMetadata>) {
        this.appsMetadata = appsMetadata
    }

    override suspend fun getPermissions(): List<PermissionInfo> = permissions
    override suspend fun setPermissions(permissions: List<PermissionInfo>) {
        this.permissions = permissions
    }

    override suspend fun getSdkSecretSeed(): String? = sdkSecretSeed
    override suspend fun setSdkSecretSeed(sdkSecretSeed: String) {
        this.sdkSecretSeed = sdkSecretSeed
    }

    override suspend fun getSdkVersion(): String? = sdkVersion
    override suspend fun setSdkVersion(sdkVersion: String) {
        this.sdkVersion = sdkVersion
    }
}