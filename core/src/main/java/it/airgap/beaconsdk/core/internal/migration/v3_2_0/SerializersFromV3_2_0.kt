@file:OptIn(ExperimentalSerializationApi::class)

package it.airgap.beaconsdk.core.internal.migration.v3_2_0

import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.data.P2pPeer
import it.airgap.beaconsdk.core.data.Peer
import it.airgap.beaconsdk.core.internal.serializer.BEACON_CORE_CLASS_DISCRIMINATOR
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonNames

private const val v3_2_0_BEACON_CORE_CLASS_DISCRIMINATOR = BEACON_CORE_CLASS_DISCRIMINATOR

// -- Peer --

internal class PeerSerializer : KSerializer<Peer> {
    override val descriptor: SerialDescriptor = PeerSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): Peer {
        val surrogate = decoder.decodeSerializableValue(PeerSurrogate.serializer())
        return surrogate.toTarget()
    }

    override fun serialize(encoder: Encoder, value: Peer) {
        val surrogate = PeerSurrogate(value)
        encoder.encodeSerializableValue(PeerSurrogate.serializer(), surrogate)
    }
}

@Serializable
private data class PeerSurrogate(
    @JsonNames(v3_2_0_BEACON_CORE_CLASS_DISCRIMINATOR) val type: Type,
    val id: String? = null,
    val name: String,
    val publicKey: String,
    val relayServer: String,
    val version: String = "1",
    val icon: String? = null,
    val appUrl: String? = null,
    val isPaired: Boolean = false,
) {
    fun toTarget(): Peer = when (type) {
        Type.P2P -> P2pPeer(id, name, publicKey, relayServer, version, icon, appUrl, isPaired)
    }

    @Serializable
    enum class Type {
        @SerialName(P2pPeer.TYPE) P2P,
    }
}

private fun PeerSurrogate(value: Peer): PeerSurrogate = with(value) {
    val type = when (this) {
        is P2pPeer -> PeerSurrogate.Type.P2P
    }

    return PeerSurrogate(type, id, name, publicKey, relayServer, version, icon, appUrl, isPaired)
}

// -- Origin --

internal class OriginSerializer : KSerializer<Origin> {
    override val descriptor: SerialDescriptor = OriginSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): Origin {
        val surrogate = decoder.decodeSerializableValue(OriginSurrogate.serializer())
        return surrogate.toTarget()
    }

    override fun serialize(encoder: Encoder, value: Origin) {
        val surrogate = OriginSurrogate(value)
        encoder.encodeSerializableValue(OriginSurrogate.serializer(), surrogate)
    }
}

@Serializable
private data class OriginSurrogate(
    @JsonNames(v3_2_0_BEACON_CORE_CLASS_DISCRIMINATOR) val type: Type,
    val id: String,
) {
    fun toTarget(): Origin = when (type) {
        Type.Website -> Origin.Website(id)
        Type.Extension -> Origin.Extension(id)
        Type.P2P -> Origin.P2P(id)
    }

    @Serializable
    enum class Type {
        @SerialName(Origin.Website.TYPE) Website,
        @SerialName(Origin.Extension.TYPE) Extension,
        @SerialName(Origin.P2P.TYPE) P2P,
    }
}

private fun OriginSurrogate(value: Origin): OriginSurrogate = with(value) {
    val type = when (this) {
        is Origin.Website -> OriginSurrogate.Type.Website
        is Origin.Extension -> OriginSurrogate.Type.Extension
        is Origin.P2P -> OriginSurrogate.Type.P2P
    }

    return OriginSurrogate(type, id)
}