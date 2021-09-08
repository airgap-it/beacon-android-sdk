package it.airgap.beaconsdk.core.internal.transport.p2p.utils

import it.airgap.beaconsdk.core.data.beacon.P2pPeer
import it.airgap.beaconsdk.core.data.beacon.Peer
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.transport.p2p.data.P2pIdentifier
import it.airgap.beaconsdk.core.internal.transport.p2p.data.P2pPairingResponse
import it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data.MatrixEvent
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalArgument
import it.airgap.beaconsdk.core.internal.utils.toHexString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class P2pCommunicator(private val app: BeaconApplication, private val crypto: Crypto) {
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

    fun pairingPayload(peer: P2pPeer, relayServer: String): Result<String> =
        runCatching {
            when (peer.version.substringBefore('.')) {
                "1" -> pairingPayloadV1()
                "2" -> pairingPayloadV2(peer, relayServer)

                // fallback to the newest version
                else -> pairingPayloadV2(peer, relayServer)
            }
        }

    fun channelOpeningMessage(recipient: String, payload: String): String =
        "@channel-open:$recipient:$payload"

    private fun pairingPayloadV1(): String = keyPair.publicKey.toHexString().asString()
    private fun pairingPayloadV2(peer: P2pPeer, relayServer: String): String =
        Json.encodeToString(
            P2pPairingResponse(
                peer.id ?: failWithInvalidPeer(peer),
                "p2p-pairing-response",
                app.name,
                peer.version,
                keyPair.publicKey.toHexString().asString(),
                relayServer,
                app.icon,
                app.url,
            )
        )

    private fun failWithInvalidPeer(peer: Peer): Nothing =
        failWithIllegalArgument("Peer $peer is invalid")
}