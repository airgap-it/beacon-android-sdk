package it.airgap.client.dapp.internal.storage.decorator

import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.client.dapp.storage.DAppClientStorage
import it.airgap.client.dapp.storage.ExtendedDAppClientStorage

internal  class DecoratedDAppClientStorage(private val plugin: DAppClientStorage) : ExtendedDAppClientStorage, DAppClientStorage by plugin {
    override suspend fun removeActivePeerId() {
        setActivePeerId(null)
    }

    override fun scoped(beaconScope: BeaconScope): ExtendedDAppClientStorage = DecoratedDAppClientStorage(plugin.scoped(beaconScope))
    override fun extend(): ExtendedDAppClientStorage = this
}