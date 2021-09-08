package it.airgap.beaconsdk.core.internal.storage

import it.airgap.beaconsdk.core.data.beacon.AppMetadata
import it.airgap.beaconsdk.core.data.beacon.Peer
import it.airgap.beaconsdk.core.data.beacon.Permission
import it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data.MatrixRoom

internal class MockStorage : Storage {
    private var p2pPeers: List<Peer> = emptyList()
    private var appsMetadata: List<AppMetadata> = emptyList()
    private var permissions: List<Permission> = emptyList()
    private var matrixRelayServer: String? = null
    private var matrixChannels: Map<String, String> = emptyMap()
    private var matrixSyncToken: String? = null
    private var matrixRooms: List<MatrixRoom> = emptyList()
    private var sdkVersion: String? = null
    private var migrations: Set<String> = emptySet()

    override suspend fun getPeers(): List<Peer> = p2pPeers
    override suspend fun setPeers(p2pPeers: List<Peer>) {
        this.p2pPeers = p2pPeers
    }

    override suspend fun getAppMetadata(): List<AppMetadata> = appsMetadata
    override suspend fun setAppMetadata(appMetadata: List<AppMetadata>) {
        this.appsMetadata = appMetadata
    }

    override suspend fun getPermissions(): List<Permission> = permissions
    override suspend fun setPermissions(permissions: List<Permission>) {
        this.permissions = permissions
    }

    override suspend fun getMatrixRelayServer(): String? = matrixRelayServer
    override suspend fun setMatrixRelayServer(relayServer: String?) {
        this.matrixRelayServer = relayServer
    }

    override suspend fun getMatrixChannels(): Map<String, String> = matrixChannels
    override suspend fun setMatrixChannels(channels: Map<String, String>) {
        this.matrixChannels = channels
    }

    override suspend fun getMatrixSyncToken(): String? = matrixSyncToken
    override suspend fun setMatrixSyncToken(syncToken: String?) {
        this.matrixSyncToken = syncToken
    }

    override suspend fun getMatrixRooms(): List<MatrixRoom> = matrixRooms
    override suspend fun setMatrixRooms(rooms: List<MatrixRoom>) {
        this.matrixRooms = rooms
    }

    override suspend fun getSdkVersion(): String? = sdkVersion
    override suspend fun setSdkVersion(sdkVersion: String) {
        this.sdkVersion = sdkVersion
    }

    override suspend fun getMigrations(): Set<String> = migrations
    override suspend fun setMigrations(migrations: Set<String>) {
        this.migrations = migrations
    }
}