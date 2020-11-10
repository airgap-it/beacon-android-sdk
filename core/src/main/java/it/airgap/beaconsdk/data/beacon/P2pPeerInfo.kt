package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * P2P peer data.
 *
 * @property [name] The name of the peer.
 * @property [publicKey] The public key of the peer.
 * @property [relayServer] The address of the server through which the peer should be communicated.
 * @property [version] The Beacon version using by the peer. If not provided, the earliest version will be assumed by default.
 * @property [icon] An optional URL for the peer icon.
 * @property [appUrl] An optional URL for the peer application.
 */
@Serializable
public data class P2pPeerInfo internal constructor(
    public val name: String,
    public val publicKey: String,
    public val relayServer: String,
    public val version: String? = null,
    public val icon: String? = null,
    public val appUrl: String? = null,
    @Transient public val isPaired: Boolean = false,
    @Transient public val isRemoved: Boolean = false,
) {
    public constructor(
        name: String,
        publicKey: String,
        relayServer: String,
        version: String? = null,
        icon: String? = null,
        appUrl: String? = null,
    ) : this(name, publicKey, relayServer, version, icon, appUrl, isPaired = false, isRemoved = false)

    public companion object {}
}