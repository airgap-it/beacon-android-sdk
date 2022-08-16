package it.airgap.beaconsdk.core.internal.transport.p2p.store

import it.airgap.beaconsdk.core.data.P2pPeer
import kotlinx.coroutines.CompletableDeferred

internal data class P2pTransportStoreState(
    val pairingPeerDeferred: CompletableDeferred<P2pPeer>? = null
)
