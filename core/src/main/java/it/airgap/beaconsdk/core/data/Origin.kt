package it.airgap.beaconsdk.core.data

import it.airgap.beaconsdk.core.internal.migration.v3_2_0.OriginSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * A group of values that identify the source of an incoming request.
 *
 * @property [id] The unique value that identifies the origin.
 */
@OptIn(ExperimentalSerializationApi::class)
@Deprecated(
    message = "Use Connection.Id instead.",
    replaceWith = ReplaceWith("Connection.Id()", imports = arrayOf("it.airgap.beaconsdk.core.data.Connection")),
    level = DeprecationLevel.WARNING,
)
@Serializable(with = OriginSerializer::class)
public sealed class Origin {
    public abstract val id: String

    @Deprecated(message = "The structure is redundant and will be removed in the next release.")
    @Serializable
    public data class Website(override val id: String) : Origin() {
        public companion object {
            internal const val TYPE = "website"
        }
    }

    @Deprecated(message = "The structure is redundant and will be removed in the next release.")
    @Serializable
    public data class Extension(override val id: String) : Origin() {
        public companion object {
            internal const val TYPE = "extension"
        }
    }

    @Deprecated(
        message = "Use Connection.Id.P2P instead.",
        replaceWith = ReplaceWith("Connection.Id.P2P(id)", imports = arrayOf("it.airgap.beaconsdk.core.data.Connection")),
        level = DeprecationLevel.WARNING,
    )
    @Serializable
    public data class P2P(override val id: String) : Origin() {
        public companion object {
            internal const val TYPE = "p2p"
        }
    }

    public companion object {
        public fun forPeer(peer: Peer): Origin =
            when (peer) {
                is P2pPeer -> P2P(peer.publicKey)
            }
    }
}