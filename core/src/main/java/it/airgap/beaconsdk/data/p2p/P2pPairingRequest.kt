package it.airgap.beaconsdk.data.p2p

import kotlinx.serialization.Serializable

@Serializable
data class P2pPairingRequest(
    val name: String,
    val publicKey: String,
    val relayServer: String,
    val icon: String? = null,
    val appUrl: String? = null
) {
    companion object {}
}