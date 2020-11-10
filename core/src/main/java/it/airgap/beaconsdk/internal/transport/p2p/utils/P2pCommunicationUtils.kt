package it.airgap.beaconsdk.internal.transport.p2p.utils

import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.transport.p2p.data.P2pHandshakeInfo
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixEvent
import it.airgap.beaconsdk.internal.utils.HexString
import it.airgap.beaconsdk.internal.utils.asHexString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class P2pCommunicationUtils(private val crypto: Crypto) {

    fun recipientIdentifier(publicKey: HexString, relayServer: String): String {
        val hash = crypto.hashKey(publicKey).value().asHexString()

        return "@${hash.value()}:$relayServer"
    }

    fun isMessageFrom(message: MatrixEvent.TextMessage, publicKey: HexString): Boolean {
        val hash = crypto.hashKey(publicKey).valueOrNull()?.asHexString()

        return message.sender.startsWith("@${hash?.value()}")
    }

    fun pairingPayload(
        version: String?,
        appName: String,
        publicKey: ByteArray,
        relayServer: String,
    ): String =
        when (version?.substringBefore('.')) {
            "1", null -> pairingPayloadV1(publicKey.asHexString())
            "2" -> pairingPayloadV2(appName, version, publicKey.asHexString(), relayServer)

            // fallback to the newest version
            else -> pairingPayloadV2(appName, version, publicKey.asHexString(), relayServer)
        }

    fun channelOpeningMessage(recipient: String, payload: String): String =
        "@channel-open:$recipient:$payload"

    private fun pairingPayloadV1(publicKey: HexString): String = publicKey.value()
    private fun pairingPayloadV2(appName: String, version: String, publicKey: HexString, relayServer: String, ): String =
        Json.encodeToString(
            P2pHandshakeInfo(
                appName,
                version,
                publicKey.value(),
                relayServer,
            )
        )
}