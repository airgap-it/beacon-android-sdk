package it.airgap.beaconsdk.chain.tezos

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.chain.tezos.internal.TezosSerializer
import it.airgap.beaconsdk.chain.tezos.internal.TezosWallet
import it.airgap.beaconsdk.core.internal.chain.Chain
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry

/**
 * Tezos implementation of the [Chain] interface.
 */
public class Tezos internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val wallet: TezosWallet,

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val serializer: TezosSerializer,
) : Chain<TezosWallet, TezosSerializer> {

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val identifier: String = IDENTIFIER

    /**
     * Factory for [Tezos].
     *
     * @constructor Creates a factory required for dynamic [Tezos] chain registration.
     */
    public class Factory : Chain.Factory<Tezos> {
        override val identifier: String = IDENTIFIER

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        override fun create(dependencyRegistry: DependencyRegistry): Tezos =
            with(dependencyRegistry) {
                val wallet = TezosWallet(crypto, base58Check)
                val serializer = TezosSerializer()

                Tezos(wallet, serializer)
            }
    }

    internal object PrefixBytes {
        val tz1: ByteArray = listOf(6, 161, 159).map(Int::toByte).toByteArray()
        val tz2: ByteArray = listOf(6, 161, 161).map(Int::toByte).toByteArray()
        val tz3: ByteArray = listOf(6, 161, 164).map(Int::toByte).toByteArray()

        val kt: ByteArray = byteArrayOf(2, 90, 121)

        val edpk: ByteArray = listOf(13, 15, 37, 217).map(Int::toByte).toByteArray()
        val edsk: ByteArray = listOf(43, 246, 78, 7).map(Int::toByte).toByteArray()
        val edsig: ByteArray = listOf(9, 245, 205, 134, 18).map(Int::toByte).toByteArray()

    }

    internal object Prefix {
        const val tz1: String = "tz1"
        const val tz2: String = "tz2"
        const val tz3: String = "tz3"

        const val kt1: String = "kt1"

        const val edpk: String = "edpk"
        const val edsk: String = "edsk"
        const val edsig: String = "edsig"
    }

    public companion object {
        internal const val IDENTIFIER = "tezos"
    }
}

/**
 * Creates a new instance of [Tezos.Factory].
 */
public fun tezos(): Tezos.Factory = Tezos.Factory()