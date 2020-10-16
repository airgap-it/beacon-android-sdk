package it.airgap.beaconsdk.compat

import it.airgap.beaconsdk.client.BeaconWalletClient
import it.airgap.beaconsdk.compat.internal.CompatStorageDecorator

fun BeaconWalletClient.Builder.storage(storage: BeaconCompatStorage): BeaconWalletClient.Builder =
    storage(CompatStorageDecorator(storage))