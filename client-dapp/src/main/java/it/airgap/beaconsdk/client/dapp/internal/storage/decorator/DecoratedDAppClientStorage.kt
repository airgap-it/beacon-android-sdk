package it.airgap.beaconsdk.client.dapp.internal.storage.decorator

import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.storage.decorator.DecoratedStorage
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.ExtendedStorage
import it.airgap.beaconsdk.client.dapp.internal.storage.plugin.ExtendedDAppClientStoragePlugin
import it.airgap.beaconsdk.client.dapp.storage.DAppClientStorage
import it.airgap.beaconsdk.client.dapp.storage.ExtendedDAppClientStorage

internal class DecoratedDAppClientStorage(
    private val storage: DAppClientStorage,
    private val beaconConfiguration: BeaconConfiguration,
): ExtendedDAppClientStorage, ExtendedStorage by DecoratedStorage(storage, beaconConfiguration), ExtendedDAppClientStoragePlugin by DecoratedDAppClientStoragePlugin(storage, beaconConfiguration) {
    override fun scoped(beaconScope: BeaconScope): ExtendedDAppClientStorage = DecoratedDAppClientStorage(storage.scoped(beaconScope), beaconConfiguration)
    override fun extend(beaconConfiguration: BeaconConfiguration): ExtendedDAppClientStorage = this
}