package it.airgap.beaconsdk.core.data.beacon

import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.network.provider.HttpProvider

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
 * @property [nodes] A list of Matrix nodes used in the connection, set to [BeaconConfiguration.defaultRelayServers] by default.
 * One node will be selected randomly based on the local key pair and used as the primary connection node,
 * the rest will be used as a fallback if the primary node goes down.
 *
 * @property [httpProvider] An optional external [HttpProvider] implementation used to make Beacon HTTP requests.
 */
public data class P2P(
    public val nodes: List<String> = BeaconConfiguration.defaultRelayServers,
    public val httpProvider: HttpProvider? = null,
) : Connection(Type.P2P) {
    init {
        require(nodes.isNotEmpty())
    }
}
