package it.airgap.beaconsdk.client.dapp.storage

import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.Storage
import it.airgap.beaconsdk.client.dapp.internal.storage.decorator.DecoratedDAppClientStorage
import it.airgap.beaconsdk.client.dapp.internal.storage.plugin.DAppClientStoragePlugin

public interface DAppClientStorage : Storage, DAppClientStoragePlugin {
    override fun scoped(beaconScope: BeaconScope): DAppClientStorage
    override fun extend(beaconConfiguration: BeaconConfiguration): ExtendedDAppClientStorage = DecoratedDAppClientStorage(this, beaconConfiguration)
}