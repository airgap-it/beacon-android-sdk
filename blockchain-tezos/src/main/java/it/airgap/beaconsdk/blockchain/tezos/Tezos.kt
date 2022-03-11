package it.airgap.beaconsdk.blockchain.tezos

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.blockchain.tezos.internal.di.ExtendedDependencyRegistry
import it.airgap.beaconsdk.blockchain.tezos.internal.di.extend
import it.airgap.beaconsdk.blockchain.tezos.internal.wallet.TezosWallet
import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry

/**
 * Tezos implementation of the [Blockchain] interface.
 */
public class Tezos internal constructor(
    internal val wallet: TezosWallet,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override val creator: Blockchain.Creator,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override val serializer: Blockchain.Serializer,
) : Blockchain {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val identifier: String = IDENTIFIER

    /**
     * Factory for [Tezos].
     *
     * @constructor Creates a factory required for dynamic [Tezos] blockchain registration.
     */
    public class Factory : Blockchain.Factory<Tezos> {
        override val identifier: String = IDENTIFIER

        private var _extendedDependencyRegistry: ExtendedDependencyRegistry? = null
        private fun extendedDependencyRegistry(dependencyRegistry: DependencyRegistry): ExtendedDependencyRegistry =
            _extendedDependencyRegistry ?: dependencyRegistry.extend().also { _extendedDependencyRegistry = it }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        override fun create(dependencyRegistry: DependencyRegistry): Tezos =
            with(extendedDependencyRegistry(dependencyRegistry)) {
                Tezos(tezosWallet, tezosCreator, tezosSerializer)
            }
    }

    internal sealed interface Prefix {
        val value: String
        val bytes: ByteArray
        val encodedLength: Int

        enum class Address(override val value: String, override val bytes: ByteArray, override val encodedLength: Int) : Prefix {
            Ed25519("tz1", listOf(6, 161, 159).map(Int::toByte).toByteArray(), encodedLength = 36), /* tz1(36) */
            Secp256K1("tz2", listOf(6, 161, 161).map(Int::toByte).toByteArray(), encodedLength = 36), /* tz2(36) */
            P256("tz3", listOf(6, 161, 164).map(Int::toByte).toByteArray(), encodedLength = 36), /* tz3(36) */
            Contract("KT1", byteArrayOf(2, 90, 121), encodedLength = 36), /* KT1(36) */
        }

        enum class PublicKey(override val value: String, override val bytes: ByteArray, override val encodedLength: Int) : Prefix {
            Ed25519("edpk", listOf(13, 15, 37, 217).map(Int::toByte).toByteArray(), encodedLength = 54), /* edpk(54) */
            Secp256K1("sppk", listOf(3, 254, 226, 86).map(Int::toByte).toByteArray(), encodedLength = 55), /* sppk(55) */
            P256("p2pk", listOf(3, 178, 139, 127).map(Int::toByte).toByteArray(), encodedLength = 55), /* p2pk(55) */
        }
    }

    public companion object {
        internal const val IDENTIFIER = "tezos"
    }
}

/**
 * Creates a new instance of [Tezos.Factory].
 */
public fun tezos(): Tezos.Factory = Tezos.Factory()