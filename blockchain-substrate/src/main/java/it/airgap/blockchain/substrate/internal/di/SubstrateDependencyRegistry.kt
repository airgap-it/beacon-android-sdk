package it.airgap.blockchain.substrate.internal.di

import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.blockchain.substrate.internal.creator.*
import it.airgap.blockchain.substrate.internal.serializer.*
import it.airgap.blockchain.substrate.internal.wallet.SubstrateWallet

internal class SubstrateDependencyRegistry(dependencyRegistry: DependencyRegistry) : ExtendedDependencyRegistry, DependencyRegistry by dependencyRegistry {

    // -- wallet --

    override val substrateWallet: SubstrateWallet by lazy { SubstrateWallet() }

    // -- creator --

    override val substrateCreator: SubstrateCreator by lazy {
        SubstrateCreator(
            DataSubstrateCreator(),
            V1BeaconMessageSubstrateCreator(),
            V2BeaconMessageSubstrateCreator(),
            V3BeaconMessageSubstrateCreator(),
        )
    }

    // -- serializer --

    override val substrateSerializer: SubstrateSerializer by lazy {
        SubstrateSerializer(
            DataSubstrateSerializer(),
            V1BeaconMessageSubstrateSerializer(),
            V2BeaconMessageSubstrateSerializer(),
            V3BeaconMessageSubstrateSerializer(),
        )
    }
}