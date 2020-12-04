package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Base for types of peers supported in Beacon.
 */
@Serializable
public sealed class Peer {
    public abstract val id: String?
    public abstract val name: String
    public abstract val publicKey: String
    public abstract val version: String

    internal abstract val isPaired: Boolean
    internal abstract val isRemoved: Boolean

    internal abstract fun paired(): Peer
    internal abstract fun removed(): Peer
}

// -- P2P --

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
@SerialName("p2p")
public data class P2pPeer internal constructor(
    override val id: String? = null,
    override val name: String,
    override val publicKey: String,
    public val relayServer: String,
    override val version: String = "1",
    public val icon: String? = null,
    public val appUrl: String? = null,
    override val isPaired: Boolean = false,
    @Transient override val isRemoved: Boolean = false,
) : Peer() {

    public constructor(
        id: String? = null,
        name: String,
        publicKey: String,
        relayServer: String,
        version: String = "1",
        icon: String? = null,
        appUrl: String? = null,
    ) : this(id, name, publicKey, relayServer, version, icon, appUrl, isPaired = false, isRemoved = false)

    public companion object {}

    override fun paired(): Peer = copy(isPaired = true)
    override fun removed(): Peer = copy(isRemoved = true)
}

// -- extensions --

@Suppress("UNCHECKED_CAST")
internal fun <T: Peer> T.selfPaired(): T = paired() as T

@Suppress("UNCHECKED_CAST")
internal fun <T: Peer> T.selfRemoved(): T = removed() as T