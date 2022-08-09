package it.airgap.client.dapp.internal.di

import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.utils.app
import it.airgap.beaconsdk.core.internal.utils.beaconSdk
import it.airgap.beaconsdk.core.internal.utils.delegate.lazyWeak
import it.airgap.client.dapp.BeaconDAppClient
import it.airgap.client.dapp.internal.controller.account.AccountController
import it.airgap.client.dapp.internal.controller.account.store.AccountControllerStore
import it.airgap.client.dapp.storage.DAppClientStorage

internal class DAppClientDependencyRegistry(dependencyRegistry: DependencyRegistry) : ExtendedDependencyRegistry, DependencyRegistry by dependencyRegistry {

    // -- client --

    private var dAppClient: BeaconDAppClient? = null
    override fun dAppClient(storage: DAppClientStorage, connections: List<Connection>, configuration: BeaconConfiguration): BeaconDAppClient {
        with(storageManager) {
            if (!hasPlugin<DAppClientStorage>()) addPlugins(storage.scoped(beaconScope).extend(configuration))
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
            configuration,
            identifierCreator,
        ).also { dAppClient = it }
    }

    // -- account --

    override val accountController: AccountController by lazyWeak { AccountController(accountControllerStore, blockchainRegistry) }
    override val accountControllerStore: AccountControllerStore by lazyWeak { AccountControllerStore(storageManager) }
}