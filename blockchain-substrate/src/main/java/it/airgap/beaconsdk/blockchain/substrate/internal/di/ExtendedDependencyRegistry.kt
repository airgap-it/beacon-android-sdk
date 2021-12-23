package it.airgap.beaconsdk.blockchain.substrate.internal.di

import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.di.findExtension
import it.airgap.beaconsdk.blockchain.substrate.internal.creator.SubstrateCreator
import it.airgap.beaconsdk.blockchain.substrate.internal.serializer.SubstrateSerializer
import it.airgap.beaconsdk.blockchain.substrate.internal.wallet.SubstrateWallet

internal interface ExtendedDependencyRegistry : DependencyRegistry {

    // -- wallet --

    val substrateWallet: SubstrateWallet

    // -- creator --

    val substrateCreator: SubstrateCreator

    // -- serializer --

    val substrateSerializer: SubstrateSerializer
}

internal fun DependencyRegistry.extend(): ExtendedDependencyRegistry =
    if (this is ExtendedDependencyRegistry) this
    else findExtension() ?: SubstrateDependencyRegistry(this).also { addExtension(it) }