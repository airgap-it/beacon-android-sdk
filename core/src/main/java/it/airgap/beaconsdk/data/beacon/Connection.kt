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
 *
 * @property [nodes] A list of Matrix nodes used in the connection, set to [BeaconConfiguration.defaultRelayServers] by default.
 * One node will be selected randomly based on the local key pair and used as the primary connection node,
 * the rest will be used as a fallback if the primary node goes down.
 */
public data class P2P(
    public val nodes: List<String> = BeaconConfiguration.defaultRelayServers
) : Connection(Type.P2P)
