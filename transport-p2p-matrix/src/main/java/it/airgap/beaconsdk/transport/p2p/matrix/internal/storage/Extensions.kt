package it.airgap.beaconsdk.transport.p2p.matrix.internal.storage

import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalState
import it.airgap.beaconsdk.transport.p2p.matrix.data.MatrixRoom
import it.airgap.beaconsdk.transport.p2p.matrix.storage.ExtendedP2pMatrixStoragePlugin
import it.airgap.beaconsdk.transport.p2p.matrix.storage.P2pMatrixStoragePlugin

internal val StorageManager.p2pMatrixPlugin: ExtendedP2pMatrixStoragePlugin
    get() = getPlugin() ?: getPlugin<P2pMatrixStoragePlugin>()?.extend()?.also { addPlugins(it) } ?: failWithMatrixStoragePluginMissing()

internal suspend fun StorageManager.getMatrixRelayServer(): String? = p2pMatrixPlugin.getMatrixRelayServer()
internal suspend fun StorageManager.setMatrixRelayServer(relayServer: String?) = p2pMatrixPlugin.setMatrixRelayServer(relayServer)

internal suspend fun StorageManager.getMatrixChannels(): Map<String, String> = p2pMatrixPlugin.getMatrixChannels()
internal suspend fun StorageManager.setMatrixChannels(channels: Map<String, String>) = p2pMatrixPlugin.setMatrixChannels(channels)

internal suspend fun StorageManager.getMatrixSyncToken(): String? = p2pMatrixPlugin.getMatrixSyncToken()
internal suspend fun StorageManager.setMatrixSyncToken(syncToken: String?) = p2pMatrixPlugin.setMatrixSyncToken(syncToken)

internal suspend fun StorageManager.getMatrixRooms(): List<MatrixRoom> = p2pMatrixPlugin.getMatrixRooms()
internal suspend fun StorageManager.setMatrixRooms(rooms: List<MatrixRoom>) = p2pMatrixPlugin.setMatrixRooms(rooms)

internal suspend fun StorageManager.removeMatrixRelayServer() = p2pMatrixPlugin.removeMatrixRelayServer()
internal suspend fun StorageManager.removeMatrixChannels() = p2pMatrixPlugin.removeMatrixChannels()
internal suspend fun StorageManager.removeMatrixSyncToken() = p2pMatrixPlugin.removeMatrixSyncToken()
internal suspend fun StorageManager.removeMatrixRooms() = p2pMatrixPlugin.removeMatrixRooms()

private fun failWithMatrixStoragePluginMissing(): Nothing = failWithIllegalState("P2P Matrix storage plugin has not been registered.")