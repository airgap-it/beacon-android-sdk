package it.airgap.client.dapp.storage

import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.Storage
import it.airgap.beaconsdk.core.storage.StoragePlugin
import it.airgap.client.dapp.internal.storage.decorator.DecoratedDAppClientStorage

public interface DAppClientStorage : Storage, StoragePlugin {
    public suspend fun getActivePeerId(): String?
    public suspend fun setActivePeerId(peerId: String?)

    override fun scoped(beaconScope: BeaconScope): DAppClientStorage
    public fun extend(): ExtendedDAppClientStorage = DecoratedDAppClientStorage(this)
}