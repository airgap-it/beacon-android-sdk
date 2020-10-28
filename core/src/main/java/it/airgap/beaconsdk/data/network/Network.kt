package it.airgap.beaconsdk.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class Network {
    abstract val name: String?
    abstract val rpcUrl: String?
    abstract val identifier: String

    @Serializable
    @SerialName(IDENTIFIER_MAINNET)
    data class Mainnet(override val name: String? = null, override val rpcUrl: String? = null) : Network() {
        @Transient
        override val identifier: String = IDENTIFIER_MAINNET

        companion object {}
    }

    @Serializable
    @SerialName(IDENTIFIER_CARTHAGENET)
    data class Carthagenet(override val name: String? = null, override val rpcUrl: String? = null) : Network() {
        @Transient
        override val identifier: String = IDENTIFIER_CARTHAGENET

        companion object {}
    }

    @Serializable
    @SerialName(IDENTIFIER_CUSTOM)
    data class Custom(override val name: String? = null, override val rpcUrl: String? = null) : Network() {
        @Transient
        override val identifier: String = IDENTIFIER_CUSTOM

        companion object {}
    }

    companion object {
        private const val IDENTIFIER_MAINNET = "mainnet"
        private const val IDENTIFIER_CARTHAGENET = "carthagenet"
        private const val IDENTIFIER_CUSTOM = "custom"
    }
}