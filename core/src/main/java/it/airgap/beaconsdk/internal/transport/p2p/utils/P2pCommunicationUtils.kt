package it.airgap.beaconsdk.internal.transport.p2p.utils

import it.airgap.beaconsdk.data.beacon.P2pPeer
import it.airgap.beaconsdk.data.beacon.Peer
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.transport.p2p.data.P2pPairingResponse
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixEvent
import it.airgap.beaconsdk.internal.utils.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class P2pCommunicationUtils(private val crypto: Crypto) {

    fun recipientIdentifier(publicKey: HexString, relayServer: String): String {
        val hash = crypto.hashKey(publicKey).get().asHexString()

        return "@${hash.asString()}:$relayServer"
    }

    fun isMessageFrom(message: MatrixEvent.TextMessage, publicKey: HexString): Boolean {
        val hash = crypto.hashKey(publicKey).getOrNull()?.asHexString()

        return message.sender.startsWith("@${hash?.asString()}")
    }

    fun pairingPayload(peer: P2pPeer, appName: String, publicKey: ByteArray, relayServer: String): InternalResult<String> =
        tryResult {
            when (peer.version.substringBefore('.')) {
                "1" -> pairingPayloadV1(publicKey.asHexString())
                "2" -> pairingPayloadV2(peer, appName, publicKey.asHexString(), relayServer)

                // fallback to the newest version
                else -> pairingPayloadV2(peer, appName, publicKey.asHexString(), relayServer)
            }
        }

    fun channelOpeningMessage(recipient: String, payload: String): String =
        "@channel-open:$recipient:$payload"

    private fun pairingPayloadV1(publicKey: HexString): String = publicKey.asString()
    private fun pairingPayloadV2(peer: P2pPeer, appName: String, publicKey: HexString, relayServer: String): String =
        Json.encodeToString(
            P2pPairingResponse(
                peer.id ?: failWithInvalidPeer(peer),
                "p2p-pairing-response",
                appName,
                peer.version,
                publicKey.asString(),
                relayServer,
            )
        )

    private fun failWithInvalidPeer(peer: Peer): Nothing =
        failWithIllegalArgument("Peer $peer is invalid")
}