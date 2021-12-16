package it.airgap.blockchain.substrate

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.blockchain.substrate.internal.SubstrateCreator
import it.airgap.blockchain.substrate.internal.SubstrateSerializer
import it.airgap.blockchain.substrate.internal.SubstrateWallet

/**
 * Substrate implementation of the [Blockchain] interface.
 */
public class Substrate(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override val wallet: Blockchain.Wallet,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override val creator: Blockchain.Creator,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override val serializer: Blockchain.Serializer,
) : Blockchain {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val identifier: String = IDENTIFIER

    /**
     * Factory for [Substrate].
     *
     * @constructor Creates a factory required for dynamic [Substrate] blockchain registration.
     */
    public class Factory : Blockchain.Factory<Substrate> {
        override val identifier: String = IDENTIFIER

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        override fun create(dependencyRegistry: DependencyRegistry): Substrate =
            with(dependencyRegistry) {
                val wallet = SubstrateWallet()
                val creator = SubstrateCreator()
                val serializer = SubstrateSerializer()

                Substrate(wallet, creator, serializer)
            }

    }

    public companion object {
        internal const val IDENTIFIER = "substrate"
    }
}

/**
 * Creates a new instance of [Substrate.Factory].
 */
public fun substrate(): Substrate.Factory = Substrate.Factory()