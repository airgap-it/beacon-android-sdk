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
 * @property [isPaired] The indication whether the client is already paired with the peer.
 * @property [icon] An optional URL for the peer icon.
 * @property [appUrl] An optional URL for the peer application.
 */
@Serializable
public data class P2pPeerInfo internal constructor(
    public val name: String,
    public val publicKey: String,
    public val relayServer: String,
    public val version: String? = null,
    @Transient public val isPaired: Boolean = false,
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