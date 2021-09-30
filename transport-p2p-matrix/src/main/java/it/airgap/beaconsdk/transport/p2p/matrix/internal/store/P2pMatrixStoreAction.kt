package it.airgap.beaconsdk.transport.p2p.matrix.internal.store

internal sealed interface P2pStoreAction

internal data class OnChannelCreated(
    val recipient: String,
    val channelId: String,
) : P2pStoreAction

internal data class OnChannelEvent(
    val sender: String,
    val channelId: String,
) : P2pStoreAction

internal data class OnChannelClosed(val channelId: String) : P2pStoreAction

internal object HardReset : P2pStoreAction