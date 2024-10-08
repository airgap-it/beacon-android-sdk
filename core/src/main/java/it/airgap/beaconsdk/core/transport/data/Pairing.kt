package it.airgap.beaconsdk.core.transport.data

import it.airgap.beaconsdk.core.data.P2pPeer
import it.airgap.beaconsdk.core.data.Peer
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalArgument
import it.airgap.beaconsdk.core.internal.utils.getString
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.jsonObject

public sealed interface PairingMessage {
    public val id: String
    public val type: String
    public val name: String
    public val version: String
    public val publicKey: String

    public fun toPeer(): Peer

    @OptIn(ExperimentalSerializationApi::class)
    public class Serializer : KJsonSerializer<PairingMessage> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PairingData") {
            element<String>("type")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): PairingMessage {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))

            return when {
                type.endsWith(PairingRequest.TYPE_SUFFIX) -> jsonDecoder.json.decodeFromJsonElement(PairingRequest.serializer(), jsonElement)
                type.endsWith(PairingResponse.TYPE_SUFFIX) -> jsonDecoder.json.decodeFromJsonElement(PairingResponse.serializer(), jsonElement)
                else -> failWithUnknownType(type)
            }
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: PairingMessage) {
            when(value) {
                is PairingRequest -> jsonEncoder.encodeSerializableValue(PairingRequest.serializer(), value)
                is PairingResponse -> jsonEncoder.encodeSerializableValue(PairingResponse.serializer(), value)
            }
        }

        private fun failWithUnknownType(type: String): Nothing = failWithIllegalArgument("Unknown pairing data type $type")
    }

    public companion object {
        public fun serializer(): KSerializer<PairingMessage> = Serializer()
    }
}

public sealed interface PairingRequest : PairingMessage {

    @OptIn(ExperimentalSerializationApi::class)
    public class Serializer : KJsonSerializer<PairingRequest> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PairingRequest") {
            element<String>("type")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): PairingRequest {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))

            return when(type) {
                P2pPairingRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(P2pPairingRequest.serializer(), jsonElement)
                else -> failWithUnknownType(type)
            }
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: PairingRequest) {
            when(value) {
                is P2pPairingRequest -> jsonEncoder.encodeSerializableValue(P2pPairingRequest.serializer(), value)
            }
        }

        private fun failWithUnknownType(type: String): Nothing = failWithIllegalArgument("Unknown pairing request type $type")
    }

    public companion object {
        internal const val TYPE_SUFFIX = "request"

        public fun serializer(): KSerializer<PairingRequest> = Serializer()
    }
}

public sealed interface PairingResponse : PairingMessage {

    @OptIn(ExperimentalSerializationApi::class)
    public class Serializer : KJsonSerializer<PairingResponse> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PairingResponse") {
            element<String>("type")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): PairingResponse {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))

            return when(type) {
                P2pPairingResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(P2pPairingResponse.serializer(), jsonElement)
                else -> failWithUnknownType(type)
            }
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: PairingResponse) {
            when(value) {
                is P2pPairingResponse -> jsonEncoder.encodeSerializableValue(P2pPairingResponse.serializer(), value)
            }
        }

        private fun failWithUnknownType(type: String): Nothing = failWithIllegalArgument("Unknown pairing response type $type")
    }

    public companion object {
        internal const val TYPE_SUFFIX = "response"

        public fun serializer(): KSerializer<PairingResponse> = Serializer()
    }
}

// -- P2P -- (TODO: Move to a separate file, currently this is the only way to keep the `when` expressions exhaustive)

public sealed interface P2pPairingMessage : PairingMessage {
    public val relayServer: String
    public val icon: String?
    public val appUrl: String?
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
public data class P2pPairingRequest(
    override val id: String,
    override val name: String,
    override val version: String,
    override val publicKey: String,
    override val relayServer: String,
    override val icon: String? = null,
    override val appUrl: String? = null,
) : P2pPairingMessage, PairingRequest {

    @EncodeDefault
    override val type: String = TYPE

    override fun toPeer(): Peer = P2pPeer(id = id, name = name, version = version, publicKey = publicKey, relayServer = relayServer, icon = icon, appUrl = appUrl)

    public companion object {
        public const val TYPE: String = "p2p-pairing-${PairingRequest.TYPE_SUFFIX}"
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
public data class P2pPairingResponse(
    override val id: String,
    override val name: String,
    override val version: String,
    override val publicKey: String,
    override val relayServer: String,
    override val icon: String? = null,
    override val appUrl: String? = null,
) : P2pPairingMessage, PairingResponse {

    @EncodeDefault
    override val type: String = TYPE

    override fun toPeer(): Peer = P2pPeer(id = id, name = name, publicKey = publicKey, relayServer = relayServer, version = version, icon = icon, appUrl = appUrl, isPaired = true)

    public companion object {
        public const val TYPE: String = "p2p-pairing-${PairingResponse.TYPE_SUFFIX}"
    }
}