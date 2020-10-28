package it.airgap.beaconsdk.data.p2p

import kotlinx.serialization.Serializable

@Serializable
data class P2pPeerInfo internal constructor(
    val name: String,
    val publicKey: String,
    val relayServer: String,
    val isPaired: Boolean = false,
    val icon: String? = null,
    val appUrl: String? = null
) {
    constructor(
        name: String,
        publicKey: String,
        relayServer: String,
        icon: String? = null,
        appUrl: String? = null
    ) : this(name, publicKey, relayServer, isPaired = false, icon, appUrl)

    companion object {}
}