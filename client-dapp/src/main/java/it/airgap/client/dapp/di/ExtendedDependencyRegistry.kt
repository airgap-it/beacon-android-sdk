package it.airgap.beaconsdk.client.wallet.internal.di

import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.di.findExtended
import it.airgap.client.dapp.BeaconDAppClient
import it.airgap.client.dapp.di.DAppDependencyRegistry

internal interface ExtendedDependencyRegistry : DependencyRegistry {

    // -- client --

    fun dAppClient(name: String, connections: List<Connection>, configuration: BeaconConfiguration): BeaconDAppClient
}

internal fun DependencyRegistry.extend(): ExtendedDependencyRegistry =
    if (this is ExtendedDependencyRegistry) this
    else findExtended<DAppDependencyRegistry>() ?: DAppDependencyRegistry(this).also { addExtended(it) }