package it.airgap.beaconsdk.storage

import it.airgap.beaconsdk.data.account.AccountInfo
import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.data.p2p.P2pPeerInfo
import it.airgap.beaconsdk.data.permission.PermissionInfo
import it.airgap.beaconsdk.compat.storage.BeaconCompatStorage
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.client.MatrixRoom

internal class MockBeaconCompatStorage :
    BeaconCompatStorage {
    private var p2pPeers: List<P2pPeerInfo> = emptyList()
    private var accounts: List<AccountInfo> = emptyList()
    private var activeAccountIdentifier: String? = null
    private var appsMetadata: List<AppMetadata> = emptyList()
    private var permissions: List<PermissionInfo> = emptyList()
    private var matrixSyncToken: String? = null
    private var matrixRooms: List<MatrixRoom> = emptyList()
    private var sdkSecretSeed: String? = null
    private var sdkVersion: String? = null

    override fun getP2pPeers(listener: BeaconCompatStorage.OnReadListener<List<P2pPeerInfo>>) {
        listener.onSuccess(p2pPeers)
    }

    override fun setP2pPeers(p2pPeers: List<P2pPeerInfo>, listener: BeaconCompatStorage.OnWriteListener) {
        this.p2pPeers = p2pPeers
        listener.onSuccess()
    }

    override fun getAccounts(listener: BeaconCompatStorage.OnReadListener<List<AccountInfo>>) {
        listener.onSuccess(accounts)
    }

    override fun setAccounts(accounts: List<AccountInfo>, listener: BeaconCompatStorage.OnWriteListener) {
        this.accounts = accounts
        listener.onSuccess()
    }

    override fun getActiveAccountIdentifier(listener: BeaconCompatStorage.OnReadListener<String?>) {
        listener.onSuccess(activeAccountIdentifier)
    }

    override fun setActiveAccountIdentifier(activeAccountIdentifier: String, listener: BeaconCompatStorage.OnWriteListener) {
        this.activeAccountIdentifier = activeAccountIdentifier
        listener.onSuccess()
    }

    override fun getAppsMetadata(listener: BeaconCompatStorage.OnReadListener<List<AppMetadata>>) {
        listener.onSuccess(appsMetadata)
    }

    override fun setAppsMetadata(appsMetadata: List<AppMetadata>, listener: BeaconCompatStorage.OnWriteListener) {
        this.appsMetadata = appsMetadata
        listener.onSuccess()
    }

    override fun getPermissions(listener: BeaconCompatStorage.OnReadListener<List<PermissionInfo>>) {
        listener.onSuccess(permissions)
    }

    override fun setPermissions(permissions: List<PermissionInfo>, listener: BeaconCompatStorage.OnWriteListener) {
        this.permissions = permissions
        listener.onSuccess()
    }

    override fun getMatrixSyncToken(listener: BeaconCompatStorage.OnReadListener<String?>) {
        listener.onSuccess(matrixSyncToken)
    }

    override fun setMatrixSyncToken(
        syncToken: String,
        listener: BeaconCompatStorage.OnWriteListener
    ) {
        this.matrixSyncToken = syncToken
        listener.onSuccess()
    }

    override fun getMatrixRooms(listener: BeaconCompatStorage.OnReadListener<List<MatrixRoom>>) {
        listener.onSuccess(matrixRooms)
    }

    override fun setMatrixRooms(
        rooms: List<MatrixRoom>,
        listener: BeaconCompatStorage.OnWriteListener
    ) {
        this.matrixRooms = rooms
        listener.onSuccess()
    }

    override fun getSdkSecretSeed(listener: BeaconCompatStorage.OnReadListener<String?>) {
        listener.onSuccess(sdkSecretSeed)
    }

    override fun setSdkSecretSeed(sdkSecretSeed: String, listener: BeaconCompatStorage.OnWriteListener) {
        this.sdkSecretSeed = sdkSecretSeed
        listener.onSuccess()
    }

    override fun getSdkVersion(listener: BeaconCompatStorage.OnReadListener<String?>) {
        listener.onSuccess(sdkVersion)
    }

    override fun setSdkVersion(sdkVersion: String, listener: BeaconCompatStorage.OnWriteListener) {
        this.sdkVersion = sdkVersion
        listener.onSuccess()
    }
}