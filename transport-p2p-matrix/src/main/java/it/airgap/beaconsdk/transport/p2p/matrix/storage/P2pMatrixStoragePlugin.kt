package it.airgap.beaconsdk.transport.p2p.matrix.storage

import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.StoragePlugin
import it.airgap.beaconsdk.transport.p2p.matrix.data.MatrixRoom
import it.airgap.beaconsdk.transport.p2p.matrix.internal.storage.decorator.DecoratedP2pMatrixStoragePlugin

public interface P2pMatrixStoragePlugin : StoragePlugin {
    public suspend fun getMatrixRelayServer(): String?
    public suspend fun setMatrixRelayServer(relayServer: String?)

    public suspend fun getMatrixChannels(): Map<String, String>
    public suspend fun setMatrixChannels(channels: Map<String, String>)

    public suspend fun getMatrixSyncToken(): String?
    public suspend fun setMatrixSyncToken(syncToken: String?)

    public suspend fun getMatrixRooms(): List<MatrixRoom>
    public suspend fun setMatrixRooms(rooms: List<MatrixRoom>)

    override fun scoped(beaconScope: BeaconScope): P2pMatrixStoragePlugin
    public fun extend(): ExtendedP2pMatrixStoragePlugin = DecoratedP2pMatrixStoragePlugin(this)
}