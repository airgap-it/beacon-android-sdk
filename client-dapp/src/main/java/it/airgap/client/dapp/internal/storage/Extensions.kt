package it.airgap.client.dapp.internal.storage

import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalState
import it.airgap.client.dapp.storage.DAppClientStorage
import it.airgap.client.dapp.storage.ExtendedDAppClientStorage

internal val StorageManager.dAppClientPlugin: ExtendedDAppClientStorage
    get() = getPlugin() ?: getPlugin<DAppClientStorage>()?.extend()?.also { addPlugins(it) } ?: failWithDAppStoragePluginMissing()

internal suspend fun StorageManager.getActivePeerId(): String? = dAppClientPlugin.getActivePeerId()
internal suspend fun StorageManager.setActivePeerId(peerId: String?) = dAppClientPlugin.setActivePeerId(peerId)

internal suspend fun StorageManager.removeActivePeerId() = dAppClientPlugin.removeActivePeerId()

private fun failWithDAppStoragePluginMissing(): Nothing = failWithIllegalState("DApp storage plugin has not been registered.")