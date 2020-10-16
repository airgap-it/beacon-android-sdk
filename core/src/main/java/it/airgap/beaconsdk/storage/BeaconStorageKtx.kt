package it.airgap.beaconsdk.storage

import it.airgap.beaconsdk.data.account.AccountInfo
import it.airgap.beaconsdk.data.p2p.P2pPairingRequest
import it.airgap.beaconsdk.data.permission.PermissionInfo
import it.airgap.beaconsdk.data.sdk.AppMetadata

interface BeaconStorageKtx {
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
}