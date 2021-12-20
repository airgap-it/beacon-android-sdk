package it.airgap.beaconsdk.blockchain.tezos.internal.di

import it.airgap.beaconsdk.blockchain.tezos.internal.creator.*
import it.airgap.beaconsdk.blockchain.tezos.internal.serializer.*
import it.airgap.beaconsdk.blockchain.tezos.internal.wallet.TezosWallet
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry

internal class TezosDependencyRegistry(dependencyRegistry: DependencyRegistry) : ExtendedDependencyRegistry, DependencyRegistry by dependencyRegistry {

    // -- wallet --

    override val tezosWallet: TezosWallet by lazy { TezosWallet(crypto, base58Check) }

    // -- creator --

    override val tezosCreator: TezosCreator by lazy {
        TezosCreator(
            DataTezosCreator(tezosWallet, storageManager, identifierCreator),
            V1BeaconMessageTezosCreator(),
            V2BeaconMessageTezosCreator(),
            V3BeaconMessageTezosCreator(),
        )
    }

    // -- serializer --

    override val tezosSerializer: TezosSerializer by lazy {
        TezosSerializer(
            DataTezosSerializer(),
            V1BeaconMessageTezosSerializer(),
            V2BeaconMessageTezosSerializer(),
            V3BeaconMessageTezosSerializer(),
        )
    }
}