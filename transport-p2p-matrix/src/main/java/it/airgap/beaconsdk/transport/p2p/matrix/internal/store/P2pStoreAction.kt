package it.airgap.beaconsdk.transport.p2p.matrix.internal.store

public sealed interface P2pStoreAction

public data class OnChannelCreated(
    val recipient: String,
    val channelId: String,
) : P2pStoreAction

public data class OnChannelEvent(
    val sender: String,
    val channelId: String,
) : P2pStoreAction

public data class OnChannelClosed(val channelId: String) : P2pStoreAction

public object HardReset : P2pStoreAction