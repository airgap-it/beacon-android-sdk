package it.airgap.beaconsdk.core.data

import it.airgap.beaconsdk.core.transport.p2p.P2pClient

/**
 * Connection types supported in Beacon.
 */
public sealed class Connection(public val type: Type) {
    public enum class Type {
        P2P
    }

    /**
     * A group of values that identify the connection.
     *
     * @property [id] The unique value that identifies the origin.
     */
    public sealed interface Id {
        public val id: String

        public data class P2P(override val id: String) : Id {
            public companion object {}
        }

        public companion object {

            public fun forPeer(peer: Peer): Id =
                when (peer) {
                    is P2pPeer -> P2P(peer.publicKey)
                }
        }
    }
}

/**
 * P2P Connection configuration.
 *
 * @property [client] A factory which creates a [P2pClient] instance that should be used for connection.
 */
public data class P2P(public val client: P2pClient.Factory<*>) : Connection(Type.P2P)
