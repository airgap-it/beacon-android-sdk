package it.airgap.client.dapp.internal.di

import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.di.findExtended
import it.airgap.client.dapp.BeaconDAppClient
import it.airgap.client.dapp.internal.controller.account.AccountController
import it.airgap.client.dapp.internal.controller.account.store.AccountControllerStore
import it.airgap.client.dapp.storage.DAppClientStorage

internal interface ExtendedDependencyRegistry : DependencyRegistry {

    // -- client --

    fun dAppClient(storage: DAppClientStorage, connections: List<Connection>, configuration: BeaconConfiguration): BeaconDAppClient

    // -- controller --

    val accountController: AccountController
    val accountControllerStore: AccountControllerStore
}

internal fun DependencyRegistry.extend(): ExtendedDependencyRegistry =
    if (this is ExtendedDependencyRegistry) this
    else findExtended<DAppClientDependencyRegistry>() ?: DAppClientDependencyRegistry(this).also { addExtended(it) }