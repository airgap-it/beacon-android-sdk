package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public sealed class Origin {
    public abstract val id: String

    @Serializable
    @SerialName("website")
    public data class Website(override val id: String) : Origin() {
        public companion object {}
    }

    @Serializable
    @SerialName("extension")
    public data class Extension(override val id: String) : Origin() {
        public companion object {}
    }

    @Serializable
    @SerialName("p2p")
    public data class P2P(override val id: String) : Origin() {
        public companion object {}
    }
}