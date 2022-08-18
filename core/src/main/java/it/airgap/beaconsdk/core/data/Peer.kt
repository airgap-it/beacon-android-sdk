package it.airgap.beaconsdk.core.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * Base for types of peers supported in Beacon.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator(Peer.CLASS_DISCRIMINATOR)
public sealed class Peer {
    public abstract val id: String?
    public abstract val name: String
    public abstract val publicKey: String
    public abstract val version: String
    public abstract val icon: String?
    public abstract val appUrl: String?

    public abstract val isPaired: Boolean
    public abstract val isRemoved: Boolean

    public abstract fun paired(): Peer
    public abstract fun removed(): Peer

    public abstract fun toConnectionId(): Connection.Id

    public companion object {
        internal const val CLASS_DISCRIMINATOR = "type"
    }
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
public data class P2pPeer(
    override val id: String? = null,
    override val name: String,
    override val publicKey: String,
    public val relayServer: String,
    override val version: String = "1",
    override val icon: String? = null,
    override val appUrl: String? = null,
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

    override fun toConnectionId(): Connection.Id = Connection.Id.P2P(publicKey)
}

// -- extensions --

@Suppress("UNCHECKED_CAST")
public fun <T : Peer> T.selfPaired(): T = paired() as T

@Suppress("UNCHECKED_CAST")
public fun <T : Peer> T.selfRemoved(): T = removed() as T