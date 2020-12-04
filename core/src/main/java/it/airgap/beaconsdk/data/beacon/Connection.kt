package it.airgap.beaconsdk.data.beacon

import it.airgap.beaconsdk.internal.BeaconConfiguration

/**
 * Connection types supported in Beacon.
 */
public sealed class Connection(internal val type: Type) {
    public enum class Type {
        P2P
    }
}

/**
 * P2P Connection configuration.
 */
public data class P2P(
    public val nodes: List<String> = BeaconConfiguration.defaultRelayServers
) : Connection(Type.P2P)
