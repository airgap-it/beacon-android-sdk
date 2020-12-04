package it.airgap.beaconsdk.internal.storage

import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.Peer
import it.airgap.beaconsdk.data.beacon.Permission
import it.airgap.beaconsdk.internal.storage.decorator.DecoratedStorage
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixRoom

internal interface Storage {
    suspend fun getPeers(): List<Peer>
    suspend fun setPeers(p2pPeers: List<Peer>)

    suspend fun getAppMetadata(): List<AppMetadata>
    suspend fun setAppMetadata(appMetadata: List<AppMetadata>)

    suspend fun getPermissions(): List<Permission>
    suspend fun setPermissions(permissions: List<Permission>)

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