package it.airgap.client.dapp.storage

import it.airgap.beaconsdk.core.data.Account
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.Storage
import it.airgap.beaconsdk.core.storage.StoragePlugin
import it.airgap.client.dapp.internal.storage.decorator.DecoratedDAppClientStorage

public interface DAppClientStorage : Storage, StoragePlugin {
    public suspend fun getActiveAccount(): Account?
    public suspend fun setActiveAccount(account: Account?)

    public suspend fun getActivePeer(): String?
    public suspend fun setActivePeer(peerId: String?)

    override fun scoped(beaconScope: BeaconScope): DAppClientStorage
    override fun extend(beaconConfiguration: BeaconConfiguration): ExtendedDAppClientStorage = DecoratedDAppClientStorage(this, beaconConfiguration)
}