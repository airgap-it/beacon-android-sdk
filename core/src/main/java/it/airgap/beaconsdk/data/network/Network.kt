package it.airgap.beaconsdk.data.network

import kotlinx.serialization.Serializable

@Serializable
sealed class Network(internal val type: Type) {
    abstract val name: String?
    abstract val rpcUrl: String?

    @Serializable
    data class Mainnet(override val name: String? = null, override val rpcUrl: String? = null) : Network(Type.Mainnet) {
        companion object {}
    }

    @Serializable
    data class Carthagenet(override val name: String? = null, override val rpcUrl: String? = null) : Network(Type.Carthagenet) {
        companion object {}
    }

    @Serializable
    data class Custom(override val name: String? = null, override val rpcUrl: String? = null) : Network(Type.Custom) {
        companion object {}
    }

    companion object {}

    internal enum class Type(val identifier: String) {
        Mainnet("mainnet"),
        Carthagenet("carthagenet"),
        Custom("custom'")
    }
}