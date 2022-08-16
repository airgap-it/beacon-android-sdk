package it.airgap.beaconsdk.transport.p2p.matrix.internal.store

internal sealed interface P2pMatrixStoreAction

internal data class OnChannelCreated(
    val recipient: String,
    val channelId: String,
) : P2pMatrixStoreAction

internal data class OnChannelEvent(
    val sender: String,
    val channelId: String,
) : P2pMatrixStoreAction

internal data class OnChannelClosed(val channelId: String) : P2pMatrixStoreAction

internal object HardReset : P2pMatrixStoreAction