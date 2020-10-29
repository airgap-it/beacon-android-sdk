package it.airgap.beaconsdk.internal.storage

import it.airgap.beaconsdk.data.beacon.AccountInfo
import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.P2pPeerInfo
import it.airgap.beaconsdk.data.beacon.PermissionInfo
import it.airgap.beaconsdk.internal.storage.decorator.DecoratedStorage
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixRoom

internal interface Storage {
    suspend fun getP2pPeers(): List<P2pPeerInfo>
    suspend fun setP2pPeers(p2pPeers: List<P2pPeerInfo>)

    suspend fun getAccounts(): List<AccountInfo>
    suspend fun setAccounts(accounts: List<AccountInfo>)

    suspend fun getActiveAccountIdentifier(): String?
    suspend fun setActiveAccountIdentifier(activeAccountIdentifier: String)

    suspend fun getAppsMetadata(): List<AppMetadata>
    suspend fun setAppsMetadata(appsMetadata: List<AppMetadata>)

    suspend fun getPermissions(): List<PermissionInfo>
    suspend fun setPermissions(permissions: List<PermissionInfo>)

    suspend fun getMatrixSyncToken(): String?
    suspend fun setMatrixSyncToken(syncToken: String)

    suspend fun getMatrixRooms(): List<MatrixRoom>
    suspend fun setMatrixRooms(rooms: List<MatrixRoom>)

    suspend fun getSdkSecretSeed(): String?
    suspend fun setSdkSecretSeed(sdkSecretSeed: String)

    suspend fun getSdkVersion(): String?
    suspend fun setSdkVersion(sdkVersion: String)

    fun extend(): ExtendedStorage = DecoratedStorage(this)
}