package it.airgap.beaconsdk.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A group of values that identify the source of an incoming request.
 *
 * @property [id] The unique value that identifies the origin.
 */
@Deprecated(
    message = "Use Connection.Id instead.",
    replaceWith = ReplaceWith("Connection.Id()", imports = arrayOf("it.airgap.beaconsdk.core.data.Connection")),
    level = DeprecationLevel.WARNING,
)
@Serializable
public sealed class Origin {
    public abstract val id: String

    @Deprecated(message = "The structure is redundant and will be removed in the next release.")
    @Serializable
    @SerialName("website")
    public data class Website(override val id: String) : Origin() {
        public companion object {}
    }

    @Deprecated(message = "The structure is redundant and will be removed in the next release.")
    @Serializable
    @SerialName("extension")
    public data class Extension(override val id: String) : Origin() {
        public companion object {}
    }

    @Deprecated(
        message = "Use Connection.Id.P2P instead.",
        replaceWith = ReplaceWith("Connection.Id.P2P(id)", imports = arrayOf("it.airgap.beaconsdk.core.data.Connection")),
        level = DeprecationLevel.WARNING,
    )
    @Serializable
    @SerialName("p2p")
    public data class P2P(override val id: String) : Origin() {
        public companion object {}
    }

    public companion object {

        public fun forPeer(peer: Peer): Origin =
            when (peer) {
                is P2pPeer -> P2P(peer.publicKey)
            }
    }
}