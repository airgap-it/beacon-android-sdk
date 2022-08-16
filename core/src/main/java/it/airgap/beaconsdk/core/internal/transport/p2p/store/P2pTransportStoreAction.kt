package it.airgap.beaconsdk.core.internal.transport.p2p.store

import it.airgap.beaconsdk.core.data.P2pPeer

internal sealed interface P2pTransportStoreAction

internal object OnPairingRequested : P2pTransportStoreAction
internal data class OnPairingCompleted(val peer: P2pPeer) : P2pTransportStoreAction
internal object DiscardPairingData : P2pTransportStoreAction