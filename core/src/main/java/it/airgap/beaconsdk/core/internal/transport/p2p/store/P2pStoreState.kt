package it.airgap.beaconsdk.core.internal.transport.p2p.store

public data class P2pStoreState(
    val relayServer: String,
    val availableNodes: Int,
    val activeChannels: Map<String, String> = mapOf(),
    val inactiveChannels: Set<String> = setOf(),
)
