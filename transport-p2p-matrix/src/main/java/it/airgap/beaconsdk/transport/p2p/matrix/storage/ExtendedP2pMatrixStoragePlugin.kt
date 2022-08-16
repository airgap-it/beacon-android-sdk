package it.airgap.beaconsdk.transport.p2p.matrix.storage

import it.airgap.beaconsdk.core.scope.BeaconScope

public interface ExtendedP2pMatrixStoragePlugin : P2pMatrixStoragePlugin {
    public suspend fun removeMatrixRelayServer()
    public suspend fun removeMatrixChannels()
    public suspend fun removeMatrixSyncToken()
    public suspend fun removeMatrixRooms()

    override fun scoped(beaconScope: BeaconScope): ExtendedP2pMatrixStoragePlugin
    override fun extend(): ExtendedP2pMatrixStoragePlugin = this
}