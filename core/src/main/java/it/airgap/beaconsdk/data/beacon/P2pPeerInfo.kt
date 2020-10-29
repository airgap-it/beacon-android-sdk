package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.Serializable

@Serializable
public data class P2pPeerInfo internal constructor(
    public val name: String,
    public val publicKey: String,
    public val relayServer: String,
    public val version: String? = null,
    public val isPaired: Boolean = false,
    public val icon: String? = null,
    public val appUrl: String? = null,
) {
    public constructor(
        name: String,
        publicKey: String,
        relayServer: String,
        version: String? = null,
        icon: String? = null,
        appUrl: String? = null,
    ) : this(name, publicKey, relayServer, version, isPaired = false, icon, appUrl)

    public companion object {}
}