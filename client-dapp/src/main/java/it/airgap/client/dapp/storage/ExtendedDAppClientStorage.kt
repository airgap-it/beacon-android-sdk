package it.airgap.client.dapp.storage

import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.ExtendedStorage

public interface ExtendedDAppClientStorage : DAppClientStorage, ExtendedStorage {
    public suspend fun removeActivePeerId()

    override fun scoped(beaconScope: BeaconScope): ExtendedDAppClientStorage
    override fun extend(): ExtendedDAppClientStorage = this
}