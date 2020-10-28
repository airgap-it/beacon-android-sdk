package it.airgap.beaconsdk.data.sdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Origin {
    abstract val id: String

    @Serializable
    @SerialName("website")
    data class Website(override val id: String) : Origin() {
        companion object {}
    }

    @Serializable
    @SerialName("extension")
    data class Extension(override val id: String) : Origin() {
        companion object {}
    }

    @Serializable
    @SerialName("p2p")
    data class P2P(override val id: String) : Origin() {
        companion object {}
    }
}