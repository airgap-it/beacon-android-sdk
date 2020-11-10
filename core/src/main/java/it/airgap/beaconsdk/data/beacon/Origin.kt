package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A group of values that identify the source of an incoming request.
 *
 * @property [id] The unique value that identifies the origin.
 */
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

    public companion object {}
}