package it.airgap.beaconsdk.client.wallet.internal.di

import it.airgap.beaconsdk.client.wallet.BeaconWalletClient
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.di.findExtended

internal interface ExtendedDependencyRegistry : DependencyRegistry {

    // -- client --

    fun walletClient(connections: List<Connection>, configuration: BeaconConfiguration): BeaconWalletClient
}

internal fun DependencyRegistry.extend(): ExtendedDependencyRegistry =
    if (this is ExtendedDependencyRegistry) this
    else findExtended<WalletClientDependencyRegistry>() ?: WalletClientDependencyRegistry(this).also { addExtended(it) }