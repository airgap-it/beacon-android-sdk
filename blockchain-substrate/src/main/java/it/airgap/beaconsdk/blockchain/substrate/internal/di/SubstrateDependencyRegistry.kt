package it.airgap.beaconsdk.blockchain.substrate.internal.di

import it.airgap.beaconsdk.blockchain.substrate.Substrate
import it.airgap.beaconsdk.blockchain.substrate.internal.creator.*
import it.airgap.beaconsdk.blockchain.substrate.internal.serializer.*
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.utils.delegate.lazyWeak

internal class SubstrateDependencyRegistry(dependencyRegistry: DependencyRegistry) : ExtendedDependencyRegistry, DependencyRegistry by dependencyRegistry {

    // -- blockchain --

    override val substrate: Substrate by lazyWeak { Substrate(substrateCreator, substrateSerializer) }

    // -- creator --

    override val substrateCreator: SubstrateCreator by lazyWeak {
        SubstrateCreator(
            DataSubstrateCreator(storageManager, identifierCreator),
            V1BeaconMessageSubstrateCreator(),
            V2BeaconMessageSubstrateCreator(),
            V3BeaconMessageSubstrateCreator(),
        )
    }

    // -- serializer --

    override val substrateSerializer: SubstrateSerializer by lazyWeak {
        SubstrateSerializer(
            DataSubstrateSerializer(),
            V1BeaconMessageSubstrateSerializer(),
            V2BeaconMessageSubstrateSerializer(),
            V3BeaconMessageSubstrateSerializer(),
        )
    }
}