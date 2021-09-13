package it.airgap.beaconsdk.transport.p2p.matrix.internal.storage

import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalState
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.MatrixRoom

internal val StorageManager.matrixPlugin: ExtendedP2pMatrixStoragePlugin
    get() = plugins.filterIsInstance<ExtendedP2pMatrixStoragePlugin>().firstOrNull()
        ?: plugins.filterIsInstance<P2pMatrixStoragePlugin>().firstOrNull()?.extend()?.also { addPlugins(it) }
        ?: failWithMatrixStoragePluginMissing()

internal suspend fun StorageManager.getMatrixRelayServer(): String? = matrixPlugin.getMatrixRelayServer()
internal suspend fun StorageManager.setMatrixRelayServer(relayServer: String?) = matrixPlugin.setMatrixRelayServer(relayServer)

internal suspend fun StorageManager.getMatrixChannels(): Map<String, String> = matrixPlugin.getMatrixChannels()
internal suspend fun StorageManager.setMatrixChannels(channels: Map<String, String>) = matrixPlugin.setMatrixChannels(channels)

internal suspend fun StorageManager.getMatrixSyncToken(): String? = matrixPlugin.getMatrixSyncToken()
internal suspend fun StorageManager.setMatrixSyncToken(syncToken: String?) = matrixPlugin.setMatrixSyncToken(syncToken)

internal suspend fun StorageManager.getMatrixRooms(): List<MatrixRoom> = matrixPlugin.getMatrixRooms()
internal suspend fun StorageManager.setMatrixRooms(rooms: List<MatrixRoom>) = matrixPlugin.setMatrixRooms(rooms)

internal suspend fun StorageManager.removeMatrixRelayServer() = matrixPlugin.removeMatrixRelayServer()
internal suspend fun StorageManager.removeMatrixChannels() = matrixPlugin.removeMatrixChannels()
internal suspend fun StorageManager.removeMatrixSyncToken() = matrixPlugin.removeMatrixSyncToken()
internal suspend fun StorageManager.removeMatrixRooms() = matrixPlugin.removeMatrixRooms()

private fun failWithMatrixStoragePluginMissing(): Nothing = failWithIllegalState("Matrix storage plugin has not been registered.")