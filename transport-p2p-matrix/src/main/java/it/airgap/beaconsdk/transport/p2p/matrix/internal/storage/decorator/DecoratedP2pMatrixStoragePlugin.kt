package it.airgap.beaconsdk.transport.p2p.matrix.internal.storage.decorator

import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.transport.p2p.matrix.storage.ExtendedP2pMatrixStoragePlugin
import it.airgap.beaconsdk.transport.p2p.matrix.storage.P2pMatrixStoragePlugin

internal class DecoratedP2pMatrixStoragePlugin(private val plugin: P2pMatrixStoragePlugin) : ExtendedP2pMatrixStoragePlugin, P2pMatrixStoragePlugin by plugin {
    override suspend fun removeMatrixRelayServer() {
        setMatrixRelayServer(null)
    }

    override suspend fun removeMatrixChannels() {
        setMatrixChannels(emptyMap())
    }

    override suspend fun removeMatrixSyncToken() {
        setMatrixSyncToken(null)
    }

    override suspend fun removeMatrixRooms() {
        setMatrixRooms(emptyList())
    }

    override fun scoped(beaconScope: BeaconScope): ExtendedP2pMatrixStoragePlugin = DecoratedP2pMatrixStoragePlugin(plugin.scoped(beaconScope))
    override fun extend(): ExtendedP2pMatrixStoragePlugin = this
}