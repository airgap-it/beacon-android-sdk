package it.airgap.beaconsdk.internal.storage

import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.Peer
import it.airgap.beaconsdk.data.beacon.Permission
import it.airgap.beaconsdk.internal.storage.decorator.DecoratedStorage
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixRoom

internal interface Storage {
    // -- Beacon --

    suspend fun getPeers(): List<Peer>
    suspend fun setPeers(p2pPeers: List<Peer>)

    suspend fun getAppMetadata(): List<AppMetadata>
    suspend fun setAppMetadata(appMetadata: List<AppMetadata>)

    suspend fun getPermissions(): List<Permission>
    suspend fun setPermissions(permissions: List<Permission>)

    // -- Matrix --

    suspend fun getMatrixRelayServer(): String?
    suspend fun setMatrixRelayServer(relayServer: String?)

    suspend fun getMatrixChannels(): Map<String, String>
    suspend fun setMatrixChannels(channels: Map<String, String>)

    suspend fun getMatrixSyncToken(): String?
    suspend fun setMatrixSyncToken(syncToken: String?)

    suspend fun getMatrixRooms(): List<MatrixRoom>
    suspend fun setMatrixRooms(rooms: List<MatrixRoom>)

    // -- SDK --

    suspend fun getSdkVersion(): String?
    suspend fun setSdkVersion(sdkVersion: String)

    suspend fun getMigrations(): Set<String>
    suspend fun setMigrations(migrations: Set<String>)

    fun extend(): ExtendedStorage = DecoratedStorage(this)
}