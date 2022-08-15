package it.airgap.beaconsdk.client.dapp.internal.storage.decorator

import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.client.dapp.internal.storage.plugin.DAppClientStoragePlugin
import it.airgap.beaconsdk.client.dapp.internal.storage.plugin.ExtendedDAppClientStoragePlugin

internal class DecoratedDAppClientStoragePlugin(
    private val plugin: DAppClientStoragePlugin,
    private val beaconConfiguration: BeaconConfiguration,
) : ExtendedDAppClientStoragePlugin, DAppClientStoragePlugin by plugin {

    override suspend fun removeActiveAccount() {
        setActiveAccount(null)
    }

    override suspend fun removeActivePeer() {
        setActivePeer(null)
    }

    override fun scoped(beaconScope: BeaconScope): ExtendedDAppClientStoragePlugin = DecoratedDAppClientStoragePlugin(plugin.scoped(beaconScope), beaconConfiguration)
    override fun extend(beaconConfiguration: BeaconConfiguration): ExtendedDAppClientStoragePlugin = this
}