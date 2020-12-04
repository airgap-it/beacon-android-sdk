package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Type of networks supported in Beacon.
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
    @SerialName(IDENTIFIER_MAINNET)
    public data class Mainnet(
        override val name: String? = null,
        override val rpcUrl: String? = null,
    ) : Network() {
        @Transient
        override val identifier: String = IDENTIFIER_MAINNET

        public companion object {}
    }

    @Serializable
    @SerialName(IDENTIFIER_CARTHAGENET)
    public data class Carthagenet(
        override val name: String? = null,
        override val rpcUrl: String? = null,
    ) : Network() {
        @Transient
        override val identifier: String = IDENTIFIER_CARTHAGENET

        public companion object {}
    }

    @Serializable
    @SerialName(IDENTIFIER_DELPHINET)
    public data class Delphinet(
        override val name: String? = null,
        override val rpcUrl: String? = null,
    ) : Network() {
        @Transient
        override val identifier: String = IDENTIFIER_DELPHINET

        public companion object {}
    }

    @Serializable
    @SerialName(IDENTIFIER_CUSTOM)
    public data class Custom(
        override val name: String? = null,
        override val rpcUrl: String? = null,
    ) : Network() {
        @Transient
        override val identifier: String = IDENTIFIER_CUSTOM

        public companion object {}
    }

    public companion object {
        private const val IDENTIFIER_MAINNET = "mainnet"
        private const val IDENTIFIER_CARTHAGENET = "carthagenet"
        private const val IDENTIFIER_DELPHINET = "delphinet"
        private const val IDENTIFIER_CUSTOM = "custom"
    }
}