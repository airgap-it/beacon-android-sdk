package it.airgap.beaconsdk.transport.p2p.matrix.internal

import it.airgap.beaconsdk.core.data.P2pPeer
import it.airgap.beaconsdk.core.data.Peer
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.transport.p2p.data.P2pIdentifier
import it.airgap.beaconsdk.core.internal.utils.decodeFromString
import it.airgap.beaconsdk.core.transport.data.P2pPairingResponse
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalArgument
import it.airgap.beaconsdk.core.internal.utils.toHexString
import it.airgap.beaconsdk.core.transport.data.P2pPairingRequest
import it.airgap.beaconsdk.core.transport.data.PairingRequest
import it.airgap.beaconsdk.core.transport.data.PairingResponse
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.MatrixEvent
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal class P2pMatrixCommunicator(private val app: BeaconApplication, private val crypto: Crypto, private val json: Json) {
    private val keyPair: KeyPair get() = app.keyPair

    fun recipientIdentifier(publicKey: ByteArray, relayServer: String): Result<P2pIdentifier> =
        runCatching {
            val hash = crypto.hashKey(publicKey).getOrThrow()
            P2pIdentifier(hash, relayServer)
        }

    fun isMessageFrom(message: MatrixEvent.TextMessage, publicKey: ByteArray): Boolean {
        val hash = crypto.hashKey(publicKey).getOrNull()?.toHexString()

        return message.sender.startsWith("@${hash?.asString()}")
    }

    fun isValidMessage(message: MatrixEvent.TextMessage): Boolean =
        crypto.validateEncryptedMessage(message.message)

    fun pairingRequest(relayServer: String): Result<P2pPairingRequest> =
        runCatching {
            P2pPairingRequest(
                id = crypto.guid().getOrThrow(),
                name = app.name,
                version = BeaconConfiguration.BEACON_VERSION,
                publicKey = keyPair.publicKey.toHexString().asString(),
                relayServer = relayServer,
                icon = app.icon,
                appUrl = app.url,
            )
        }

    fun pairingResponse(request: P2pPairingRequest, relayServer: String): P2pPairingResponse =
        P2pPairingResponse(
            id = request.id,
            name = app.name,
            version = BeaconConfiguration.BEACON_VERSION,
            publicKey = keyPair.publicKey.toHexString().asString(),
            relayServer = relayServer,
            icon = app.icon,
            appUrl = app.url
        )

    fun pairingResponsePayload(peer: P2pPeer, relayServer: String): Result<String> =
        runCatching {
            val pairingRequest = P2pPairingRequest.from(peer)
            val pairingResponse = pairingResponse(pairingRequest, relayServer)

            pairingResponse.payload()
        }

    fun pairingResponseFromPayload(payload: String): P2pPairingResponse =
        when (json.decodeFromString<JsonElement>(payload)) {
            is JsonPrimitive /* v1 */ -> P2pPairingResponse(
                id = "",
                name = "",
                version = "1",
                publicKey = payload,
                relayServer = "",
            )
            else -> json.decodeFromString(payload)
        }


    fun createChannelOpeningMessage(recipient: String, payload: String): String =
        "${MESSAGE_HEADER_CHANNEL_OPEN}${MESSAGE_SEPARATOR_CHANNEL_OPEN}$recipient${MESSAGE_SEPARATOR_CHANNEL_OPEN}$payload"

    fun recognizeChannelOpeningMessage(message: String): Boolean = message.startsWith(MESSAGE_HEADER_CHANNEL_OPEN)

    fun destructChannelOpeningMessage(message: String): Result<Pair<String, String>> =
        runCatching {
            if (!recognizeChannelOpeningMessage(message)) failWithInvalidChannelOpeningMessage(message)

            val components = message.split(MESSAGE_SEPARATOR_CHANNEL_OPEN)
            if (components.size < 3) failWithInvalidChannelOpeningMessage(message)

            val recipient = components.subList(1, components.lastIndex - 1).joinToString(MESSAGE_SEPARATOR_CHANNEL_OPEN)
            val payload = components[components.lastIndex]

            Pair(recipient, payload)
        }

    private fun P2pPairingResponse.payload(): String =
        when (version.substringBefore('.')) {
            "1" -> v1Payload()
            "2" -> v2Payload()
            "3" -> v3Payload()

            // fallback to the newest version
            else -> v3Payload()
        }

    private fun P2pPairingResponse.v1Payload(): String = publicKey
    private fun P2pPairingResponse.v2Payload(): String = json.encodeToString(this)
    private fun P2pPairingResponse.v3Payload(): String = v2Payload()

    private fun P2pPairingRequest.Companion.from(peer: P2pPeer): P2pPairingRequest = with(peer) {
        P2pPairingRequest(
            id = id ?: failWithInvalidPeer(peer),
            name = name,
            version = version,
            publicKey = publicKey,
            relayServer = relayServer,
            icon = icon,
            appUrl = appUrl,
        )
    }

    private fun failWithInvalidPeer(peer: Peer): Nothing =
        failWithIllegalArgument("Peer $peer is invalid")

    private fun failWithInvalidChannelOpeningMessage(message: String): Nothing =
        failWithIllegalArgument("Channel opening message $message is invalid")

    companion object {
        private const val MESSAGE_HEADER_CHANNEL_OPEN = "@channel-open"
        private const val MESSAGE_SEPARATOR_CHANNEL_OPEN = ":"
    }
}