package it.airgap.client.dapp.internal.storage

import it.airgap.beaconsdk.core.data.Account
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalState
import it.airgap.client.dapp.storage.DAppClientStorage
import it.airgap.client.dapp.storage.ExtendedDAppClientStorage

internal val StorageManager.dAppClientPlugin: ExtendedDAppClientStorage
    get() = getPlugin() ?: getPlugin<DAppClientStorage>()?.extend(beaconConfiguration)?.also { addPlugins(it) } ?: failWithDAppStoragePluginMissing()

internal suspend fun StorageManager.getActiveAccount(): Account? = dAppClientPlugin.getActiveAccount()
internal suspend fun StorageManager.setActiveAccount(account: Account?) = dAppClientPlugin.setActiveAccount(account)

internal suspend fun StorageManager.removeActiveAccount() = dAppClientPlugin.removeActiveAccount()

internal suspend fun StorageManager.getActivePeer(): String? = dAppClientPlugin.getActivePeer()
internal suspend fun StorageManager.setActivePeer(publicKey: String?) = dAppClientPlugin.setActivePeer(publicKey)

internal suspend fun StorageManager.removeActivePeer() = dAppClientPlugin.removeActivePeer()

private fun failWithDAppStoragePluginMissing(): Nothing = failWithIllegalState("DApp storage plugin has not been registered.")