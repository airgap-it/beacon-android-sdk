package it.airgap.client.dapp.internal.storage.plugin

import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.ExtendedStorage

public interface ExtendedDAppClientStoragePlugin : DAppClientStoragePlugin {
    public suspend fun removeActiveAccount()
    public suspend fun removeActivePeer()

    override fun scoped(beaconScope: BeaconScope): ExtendedDAppClientStoragePlugin
    override fun extend(beaconConfiguration: BeaconConfiguration): ExtendedDAppClientStoragePlugin = this
}