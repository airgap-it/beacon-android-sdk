package it.airgap.beaconsdk.client.dapp.storage

import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.ExtendedStorage
import it.airgap.beaconsdk.client.dapp.internal.storage.plugin.ExtendedDAppClientStoragePlugin

public interface ExtendedDAppClientStorage : ExtendedStorage, DAppClientStorage, ExtendedDAppClientStoragePlugin {
    override fun scoped(beaconScope: BeaconScope): ExtendedDAppClientStorage
    override fun extend(beaconConfiguration: BeaconConfiguration): ExtendedDAppClientStorage = this
}