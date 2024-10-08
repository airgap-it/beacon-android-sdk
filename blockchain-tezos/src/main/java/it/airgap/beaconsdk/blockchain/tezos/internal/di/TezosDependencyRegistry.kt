package it.airgap.beaconsdk.blockchain.tezos.internal.di

import it.airgap.beaconsdk.blockchain.tezos.Tezos
import it.airgap.beaconsdk.blockchain.tezos.internal.creator.*
import it.airgap.beaconsdk.blockchain.tezos.internal.serializer.*
import it.airgap.beaconsdk.blockchain.tezos.internal.wallet.TezosWallet
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.utils.delegate.lazyWeak

internal class TezosDependencyRegistry(dependencyRegistry: DependencyRegistry) : ExtendedDependencyRegistry, DependencyRegistry by dependencyRegistry {

    // -- blockchain --

    override val tezos: Tezos by lazyWeak { Tezos(tezosWallet, tezosCreator, tezosSerializer) }

    // -- wallet --

    override val tezosWallet: TezosWallet by lazyWeak { TezosWallet(crypto, base58Check) }

    // -- creator --

    override val tezosCreator: TezosCreator by lazyWeak {
        TezosCreator(
            DataTezosCreator(storageManager, identifierCreator),
            V1BeaconMessageTezosCreator(),
            V2BeaconMessageTezosCreator(),
            V3BeaconMessageTezosCreator(),
        )
    }

    // -- serializer --

    override val tezosSerializer: TezosSerializer by lazyWeak {
        TezosSerializer(
            DataTezosSerializer(),
            V1BeaconMessageTezosSerializer(),
            V2BeaconMessageTezosSerializer(),
            V3BeaconMessageTezosSerializer(),
        )
    }
}