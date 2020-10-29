package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
        private const val IDENTIFIER_CUSTOM = "custom"
    }
}