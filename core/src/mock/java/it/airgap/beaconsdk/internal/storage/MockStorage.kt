package it.airgap.beaconsdk.internal.storage

import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.Peer
import it.airgap.beaconsdk.data.beacon.Permission
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixRoom

internal class MockStorage : Storage {
    private var p2pPeers: List<Peer> = emptyList()
    private var appsMetadata: List<AppMetadata> = emptyList()
    private var permissions: List<Permission> = emptyList()
    private var matrixSyncToken: String? = null
    private var matrixRooms: List<MatrixRoom> = emptyList()
    private var sdkVersion: String? = null

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

    override suspend fun getMatrixSyncToken(): String? = matrixSyncToken
    override suspend fun setMatrixSyncToken(syncToken: String) {
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
}