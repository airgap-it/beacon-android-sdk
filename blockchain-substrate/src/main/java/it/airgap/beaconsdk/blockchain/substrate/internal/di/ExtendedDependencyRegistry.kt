package it.airgap.beaconsdk.blockchain.substrate.internal.di

import it.airgap.beaconsdk.blockchain.substrate.Substrate
import it.airgap.beaconsdk.blockchain.substrate.internal.creator.SubstrateCreator
import it.airgap.beaconsdk.blockchain.substrate.internal.serializer.SubstrateSerializer
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.di.findExtended

internal interface ExtendedDependencyRegistry : DependencyRegistry {

    // -- blockchain --

    val substrate: Substrate

    // -- creator --

    val substrateCreator: SubstrateCreator

    // -- serializer --

    val substrateSerializer: SubstrateSerializer
}

internal fun DependencyRegistry.extend(): ExtendedDependencyRegistry =
    if (this is ExtendedDependencyRegistry) this
    else findExtended<SubstrateDependencyRegistry>() ?: SubstrateDependencyRegistry(this).also { addExtended(it) }