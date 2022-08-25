package it.airgap.beaconsdk.client.dapp.internal.storage.plugin

import it.airgap.beaconsdk.client.dapp.data.PairedAccount
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.StoragePlugin
import it.airgap.beaconsdk.client.dapp.internal.storage.decorator.DecoratedDAppClientStoragePlugin

public interface DAppClientStoragePlugin : StoragePlugin {
    public suspend fun getActiveAccount(): PairedAccount?
    public suspend fun setActiveAccount(account: PairedAccount?)

    public suspend fun getActivePeer(): String?
    public suspend fun setActivePeer(peerId: String?)

    override fun scoped(beaconScope: BeaconScope): DAppClientStoragePlugin
    public fun extend(beaconConfiguration: BeaconConfiguration): ExtendedDAppClientStoragePlugin = DecoratedDAppClientStoragePlugin(this, beaconConfiguration)
}