package it.airgap.beaconsdk.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

// TODO: opt for a new `Permission` structure, add migration and move Tezos networks from here to `chain-tezos`.
/**
 * Base for networks supported in Beacon.
 *
 * @property [name] An optional name of the network.
 * @property [rpcUrl] An optional URL for the network RPC interface.
 * @property [identifier] A unique value that identifies the network.
 */
@Serializable
public sealed class Network {
    public abstract val name: String?
    public abstract val rpcUrl: String?
    public abstract val identifier: String

    @Serializable
    @SerialName(Mainnet.IDENTIFIER)
    public data class Mainnet(
        override val name: String? = null,
        override val rpcUrl: String? = null,
    ) : Network() {
        @Transient
        override val identifier: String = IDENTIFIER

        public companion object {
            internal const val IDENTIFIER = "mainnet"
        }
    }

    @Serializable
    @SerialName(Delphinet.IDENTIFIER)
    public data class Delphinet(
        override val name: String? = null,
        override val rpcUrl: String? = null,
    ) : Network() {
        @Transient
        override val identifier: String = IDENTIFIER

        public companion object {
            internal const val IDENTIFIER = "delphinet"
        }
    }

    @Serializable
    @SerialName(Edonet.IDENTIFIER)
    public data class Edonet(
        override val name: String? = null,
        override val rpcUrl: String? = null,
    ) : Network() {
        @Transient
        override val identifier: String = IDENTIFIER

        public companion object {
            internal const val IDENTIFIER = "edonet"
        }
    }

    @Serializable
    @SerialName(Florencenet.IDENTIFIER)
    public data class Florencenet(
        override val name: String? = null,
        override val rpcUrl: String? = null,
    ) : Network() {
        @Transient
        override val identifier: String = IDENTIFIER

        public companion object {
            internal const val IDENTIFIER = "florencenet"
        }
    }

    @Serializable
    @SerialName(Granadanet.IDENTIFIER)
    public data class Granadanet(
        override val name: String? = null,
        override val rpcUrl: String? = null,
    ) : Network() {
        @Transient
        override val identifier: String = IDENTIFIER

        public companion object {
            internal const val IDENTIFIER = "granadanet"
        }
    }

    @Serializable
    @SerialName(Custom.IDENTIFIER)
    public data class Custom(
        override val name: String? = null,
        override val rpcUrl: String? = null,
    ) : Network() {
        @Transient
        override val identifier: String = IDENTIFIER

        public companion object {
            internal const val IDENTIFIER = "custom"
        }
    }

    public companion object {}
}