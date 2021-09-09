package it.airgap.beaconsdk.chain.tezos

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.chain.tezos.internal.message.VersionedTezosMessage
import it.airgap.beaconsdk.chain.tezos.internal.wallet.Wallet
import it.airgap.beaconsdk.core.internal.chain.Chain
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry

public class Tezos internal constructor(
    override val wallet: Wallet,
    override val versionedMessage: VersionedTezosMessage
) : Chain<Wallet, VersionedTezosMessage> {

    public class Factory internal constructor(): Chain.Factory<Tezos> {
        override val identifier: String = IDENTIFIER

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        override fun create(dependencyRegistry: DependencyRegistry): Tezos =
            with(dependencyRegistry) {
                val wallet = Wallet(crypto, base58Check)
                val versionedMessage = VersionedTezosMessage()

                Tezos(wallet, versionedMessage)
            }
    }

    internal object PrefixBytes {
        val tz1: ByteArray = byteArrayOf(6, 161.toByte(), 159.toByte())
    }

    internal object Prefix {
        const val edpk: String = "edpk"
    }

    public companion object {
        internal const val IDENTIFIER = "tezos"
    }
}

private var factory: Tezos.Factory? = null

public fun tezos(): Tezos.Factory = factory ?: Tezos.Factory().also { factory = it }