package it.airgap.beaconsdk.core.storage

import it.airgap.beaconsdk.core.scope.BeaconScope

public interface StoragePlugin {
    public fun scoped(beaconScope: BeaconScope): StoragePlugin
}