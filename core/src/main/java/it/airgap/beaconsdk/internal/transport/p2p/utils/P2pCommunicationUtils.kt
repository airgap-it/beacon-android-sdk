package it.airgap.beaconsdk.internal.transport.p2p.utils

import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.client.MatrixEvent
import it.airgap.beaconsdk.internal.utils.HexString
import it.airgap.beaconsdk.internal.utils.asHexString

internal class P2pCommunicationUtils(private val crypto: Crypto) {

    fun getRecipientIdentifier(publicKey: HexString, relayServer: String): String {
        val hash = crypto.hashKey(publicKey).getOrThrow().asHexString()

        return "@${hash.value(withPrefix = false)}:$relayServer"
    }

    fun isMessageFrom(message: MatrixEvent.TextMessage, publicKey: HexString): Boolean {
        val hash = crypto.hashKey(publicKey).getOrNull()?.asHexString()

        return message.sender.startsWith("@${hash?.value(withPrefix = false)}")
    }

    fun createOpenChannelMessage(recipient: String, handshakeInfo: String): String =
        "@channel-open:$recipient:$handshakeInfo"
}