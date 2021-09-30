package it.airgap.beaconsdk.core.data

import it.airgap.beaconsdk.core.transport.p2p.P2pClient

/**
 * Connection types supported in Beacon.
 */
public sealed class Connection(public val type: Type) {
    public enum class Type {
        P2P
    }
}

/**
 * P2P Connection configuration.
 *
 * @property [client] A factory which creates a [P2pClient] instance that should be used for connection.
 */
public data class P2P(public val client: P2pClient.Factory<*>) : Connection(Type.P2P)
