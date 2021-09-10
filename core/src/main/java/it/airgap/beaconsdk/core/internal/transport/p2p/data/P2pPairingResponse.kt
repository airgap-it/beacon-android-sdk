package it.airgap.beaconsdk.core.internal.transport.p2p.data

import kotlinx.serialization.Serializable

@Serializable
public data class P2pPairingResponse(
    val id: String,
    val type: String,
    val name: String,
    val version: String,
    val publicKey: String,
    val relayServer: String,
    val icon: String? = null,
    val appUrl: String? = null,
)