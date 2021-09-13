package it.airgap.beaconsdk.transport.p2p.matrix.internal.storage

public interface ExtendedP2pMatrixStoragePlugin : P2pMatrixStoragePlugin {
    public suspend fun removeMatrixRelayServer()
    public suspend fun removeMatrixChannels()
    public suspend fun removeMatrixSyncToken()
    public suspend fun removeMatrixRooms()

    override fun extend(): ExtendedP2pMatrixStoragePlugin = this
}