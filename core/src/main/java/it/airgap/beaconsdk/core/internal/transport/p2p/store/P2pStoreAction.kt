package it.airgap.beaconsdk.core.internal.transport.p2p.store

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