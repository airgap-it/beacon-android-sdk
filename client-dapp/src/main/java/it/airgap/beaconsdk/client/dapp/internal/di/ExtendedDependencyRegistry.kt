package it.airgap.beaconsdk.client.dapp.internal.di

import it.airgap.beaconsdk.client.dapp.BeaconDAppClient
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.di.findExtended
import it.airgap.beaconsdk.client.dapp.internal.controller.account.AccountController
import it.airgap.beaconsdk.client.dapp.internal.controller.account.store.AccountControllerStore
import it.airgap.beaconsdk.client.dapp.internal.storage.plugin.DAppClientStoragePlugin

internal interface ExtendedDependencyRegistry : DependencyRegistry {

    // -- client --

    fun dAppClient(storagePlugin: DAppClientStoragePlugin, connections: List<Connection>, configuration: BeaconConfiguration): BeaconDAppClient

    // -- controller --

    val accountController: AccountController
    val accountControllerStore: AccountControllerStore
}

internal fun DependencyRegistry.extend(): ExtendedDependencyRegistry =
    if (this is ExtendedDependencyRegistry) this
    else findExtended<DAppClientDependencyRegistry>() ?: DAppClientDependencyRegistry(this).also { addExtended(it) }