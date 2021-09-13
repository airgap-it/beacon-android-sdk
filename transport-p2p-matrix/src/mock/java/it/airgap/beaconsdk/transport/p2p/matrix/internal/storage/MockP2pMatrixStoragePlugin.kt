package it.airgap.beaconsdk.transport.p2p.matrix.internal.storage

import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.MatrixRoom

internal class MockP2pMatrixStoragePlugin : P2pMatrixStoragePlugin {
    private var matrixRelayServer: String? = null
    private var matrixChannels: Map<String, String> = emptyMap()
    private var matrixSyncToken: String? = null
    private var matrixRooms: List<MatrixRoom> = emptyList()

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
}