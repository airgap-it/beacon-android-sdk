package it.airgap.beaconsdk.client.wallet.internal.di

import it.airgap.beaconsdk.client.wallet.BeaconWalletClient
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.utils.beaconSdk

internal class WalletDependencyRegistry(dependencyRegistry: DependencyRegistry) : ExtendedDependencyRegistry, DependencyRegistry by dependencyRegistry {

    // -- client --

    private var walletClient: BeaconWalletClient? = null
    override fun walletClient(name: String, connections: List<Connection>, configuration: BeaconConfiguration): BeaconWalletClient =
        walletClient ?: BeaconWalletClient(
            name,
            beaconSdk.beaconId(beaconScope),
            beaconScope,
            connectionController(connections),
            messageController,
            storageManager,
            crypto,
            configuration,
        ).also { walletClient = it }
}