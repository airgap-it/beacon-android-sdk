package it.airgap.beaconsdk.blockchain.tezos.internal.di

import it.airgap.beaconsdk.blockchain.tezos.Tezos
import it.airgap.beaconsdk.blockchain.tezos.internal.creator.TezosCreator
import it.airgap.beaconsdk.blockchain.tezos.internal.serializer.TezosSerializer
import it.airgap.beaconsdk.blockchain.tezos.internal.wallet.TezosWallet
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.di.findExtended

internal interface ExtendedDependencyRegistry : DependencyRegistry {

    // -- blockchain --

    val tezos: Tezos

    // -- wallet --

    val tezosWallet: TezosWallet

    // -- creator --

    val tezosCreator: TezosCreator

    // -- serializer --

    val tezosSerializer: TezosSerializer
}

internal fun DependencyRegistry.extend(): ExtendedDependencyRegistry =
    if (this is ExtendedDependencyRegistry) this
    else findExtended<TezosDependencyRegistry>() ?: TezosDependencyRegistry(this).also { addExtended(it) }