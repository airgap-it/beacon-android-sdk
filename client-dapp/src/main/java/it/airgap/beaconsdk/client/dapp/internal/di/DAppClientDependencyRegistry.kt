package it.airgap.beaconsdk.client.dapp.internal.di

import it.airgap.beaconsdk.client.dapp.BeaconDAppClient
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.utils.app
import it.airgap.beaconsdk.core.internal.utils.beaconSdk
import it.airgap.beaconsdk.core.internal.utils.delegate.lazyWeak
import it.airgap.beaconsdk.client.dapp.internal.controller.account.AccountController
import it.airgap.beaconsdk.client.dapp.internal.controller.account.store.AccountControllerStore
import it.airgap.beaconsdk.client.dapp.internal.storage.plugin.DAppClientStoragePlugin

internal class DAppClientDependencyRegistry(dependencyRegistry: DependencyRegistry) : ExtendedDependencyRegistry, DependencyRegistry by dependencyRegistry {

    // -- client --

    private var dAppClient: BeaconDAppClient? = null
    override fun dAppClient(storagePlugin: DAppClientStoragePlugin, connections: List<Connection>, configuration: BeaconConfiguration): BeaconDAppClient {
        with(storageManager) {
            if (!hasPlugin<DAppClientStoragePlugin>()) addPlugins(storagePlugin.scoped(beaconScope).extend(configuration))
        }

        return dAppClient ?: BeaconDAppClient(
            app(beaconScope),
            beaconSdk.beaconId(beaconScope),
            beaconScope,
            connectionController(connections),
            messageController,
            accountController,
            storageManager,
            crypto,
            serializer,
            identifierCreator,
            configuration,
        ).also { dAppClient = it }
    }

    // -- account --

    override val accountController: AccountController by lazyWeak { AccountController(accountControllerStore, blockchainRegistry) }
    override val accountControllerStore: AccountControllerStore by lazyWeak { AccountControllerStore(storageManager) }
}