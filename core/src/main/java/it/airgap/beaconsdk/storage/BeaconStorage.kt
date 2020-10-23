package it.airgap.beaconsdk.storage

import it.airgap.beaconsdk.data.account.AccountInfo
import it.airgap.beaconsdk.data.p2p.P2pPeerInfo
import it.airgap.beaconsdk.data.permission.PermissionInfo
import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.client.MatrixRoom

interface BeaconStorage {
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
}