package it.airgap.beaconsdk.internal.transport.p2p.store

internal data class P2pStoreState(
    val relayServer: String,
    val availableNodes: Int,
    val activeChannels: Map<String, String> = mapOf(),
    val inactiveChannels: Set<String> = setOf(),
)
