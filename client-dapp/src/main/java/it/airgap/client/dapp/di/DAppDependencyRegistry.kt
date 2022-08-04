package it.airgap.client.dapp.di

import it.airgap.beaconsdk.client.wallet.internal.di.ExtendedDependencyRegistry
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.utils.beaconSdk
import it.airgap.client.dapp.BeaconDAppClient

internal class DAppDependencyRegistry(dependencyRegistry: DependencyRegistry) : ExtendedDependencyRegistry, DependencyRegistry by dependencyRegistry {

    // -- client --

    private var dAppClient: BeaconDAppClient? = null
    override fun dAppClient(name: String, connections: List<Connection>, configuration: BeaconConfiguration): BeaconDAppClient =
        dAppClient ?: BeaconDAppClient(
            name,
            beaconSdk.beaconId(beaconScope),
            beaconScope,
            connectionController(connections),
            messageController,
            storageManager,
            crypto,
            configuration,
        ).also { dAppClient = it }
}