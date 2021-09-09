package it.airgap.beaconsdk.core.internal.storage

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.beacon.AppMetadata
import it.airgap.beaconsdk.core.data.beacon.Peer
import it.airgap.beaconsdk.core.data.beacon.Permission
import it.airgap.beaconsdk.core.internal.storage.decorator.DecoratedStorage
import it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data.MatrixRoom

public interface Storage {
    // -- Beacon --

    public suspend fun getPeers(): List<Peer>
    public suspend fun setPeers(p2pPeers: List<Peer>)

    public suspend fun getAppMetadata(): List<AppMetadata>
    public suspend fun setAppMetadata(appMetadata: List<AppMetadata>)

    public suspend fun getPermissions(): List<Permission>
    public suspend fun setPermissions(permissions: List<Permission>)

    // -- Matrix --

    public suspend fun getMatrixRelayServer(): String?
    public suspend fun setMatrixRelayServer(relayServer: String?)

    public suspend fun getMatrixChannels(): Map<String, String>
    public suspend fun setMatrixChannels(channels: Map<String, String>)

    public suspend fun getMatrixSyncToken(): String?
    public suspend fun setMatrixSyncToken(syncToken: String?)

    public suspend fun getMatrixRooms(): List<MatrixRoom>
    public suspend fun setMatrixRooms(rooms: List<MatrixRoom>)

    // -- SDK --

    public suspend fun getSdkVersion(): String?
    public suspend fun setSdkVersion(sdkVersion: String)

    public suspend fun getMigrations(): Set<String>
    public suspend fun setMigrations(migrations: Set<String>)

    public fun extend(): ExtendedStorage = DecoratedStorage(this)
}