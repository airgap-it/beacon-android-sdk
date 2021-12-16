package it.airgap.beaconsdk.blockchain.tezos.data

import it.airgap.beaconsdk.blockchain.tezos.Tezos
import it.airgap.beaconsdk.core.data.Network
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Type of Tezos networks supported in Beacon.
 *
 * @property [blockchainIdentifier] A unique name of the blockchain which the network applies to.
 * @property [type] A unique value that identifies the network.
 * @property [name] An optional name of the network.
 * @property [rpcUrl] An optional URL for the network RPC interface.
 */
@Serializable
public sealed class TezosNetwork : Network() {
    @Transient
    override val blockchainIdentifier: String = Tezos.IDENTIFIER

    public abstract val type: String

    override val identifier: String
        get() = mutableListOf(type).apply {
            name?.let { add("name:$it") }
            rpcUrl?.let { add("rpc:$it") }
        }.joinToString("-")

    @Serializable
    @SerialName(Mainnet.TYPE)
    public data class Mainnet(
        override val name: String? = null,
        override val rpcUrl: String? = null,
    ) : TezosNetwork() {
        @Transient
        override val type: String = TYPE

        public companion object {
            internal const val TYPE = "mainnet"
        }
    }

    @Deprecated("'Delphinet' is no longer a maintained Tezos test network and will be removed from Beacon in future versions.")
    @Serializable
    @SerialName(Delphinet.TYPE)
    public data class Delphinet(
        override val name: String? = null,
        override val rpcUrl: String? = null,
    ) : TezosNetwork() {
        @Transient
        override val type: String = TYPE

        public companion object {
            internal const val TYPE = "delphinet"
        }
    }

    @Deprecated("'Edonet' is no longer a maintained Tezos test network and will be removed from Beacon in future versions.")
    @Serializable
    @SerialName(Edonet.TYPE)
    public data class Edonet(
        override val name: String? = null,
        override val rpcUrl: String? = null,
    ) : TezosNetwork() {
        @Transient
        override val type: String = TYPE

        public companion object {
            internal const val TYPE = "edonet"
        }
    }

    @Deprecated("'Florencenet' is no longer a maintained Tezos test network and will be removed from Beacon in future versions.")
    @Serializable
    @SerialName(Florencenet.TYPE)
    public data class Florencenet(
        override val name: String? = null,
        override val rpcUrl: String? = null,
    ) : TezosNetwork() {
        @Transient
        override val type: String = TYPE

        public companion object {
            internal const val TYPE = "florencenet"
        }
    }

    @Serializable
    @SerialName(Granadanet.TYPE)
    public data class Granadanet(
        override val name: String? = null,
        override val rpcUrl: String? = null,
    ) : TezosNetwork() {
        @Transient
        override val type: String = TYPE

        public companion object {
            internal const val TYPE = "granadanet"
        }
    }

    @Serializable
    @SerialName(Hangzhounet.TYPE)
    public data class Hangzhounet(
        override val name: String? = null,
        override val rpcUrl: String? = null,
    ) : TezosNetwork() {
        @Transient
        override val type: String = TYPE

        public companion object {
            internal const val TYPE = "hangzhounet"
        }
    }

    @Serializable
    @SerialName(Idiazabal.TYPE)
    public data class Idiazabal(
        override val name: String? = null,
        override val rpcUrl: String? = null,
    ) : TezosNetwork() {
        @Transient
        override val type: String = TYPE

        public companion object {
            internal const val TYPE = "idiazabalnet"
        }
    }


    @Serializable
    @SerialName(Custom.TYPE)
    public data class Custom(
        override val name: String? = null,
        override val rpcUrl: String? = null,
    ) : TezosNetwork() {
        @Transient
        override val type: String = TYPE

        public companion object {
            internal const val TYPE = "custom"
        }
    }

    public companion object {}
}
