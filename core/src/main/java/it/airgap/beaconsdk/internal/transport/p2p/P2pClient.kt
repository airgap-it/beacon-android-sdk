package it.airgap.beaconsdk.internal.transport.p2p

import it.airgap.beaconsdk.internal.BeaconConfig
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.internal.crypto.data.SessionKeyPair
import it.airgap.beaconsdk.internal.transport.p2p.data.P2pHandshakeInfo
import it.airgap.beaconsdk.internal.transport.p2p.data.P2pMessage
import it.airgap.beaconsdk.internal.transport.p2p.matrix.MatrixClient
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.client.MatrixEvent
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.client.MatrixRoom
import it.airgap.beaconsdk.internal.transport.p2p.utils.P2pCommunicationUtils
import it.airgap.beaconsdk.internal.transport.p2p.utils.P2pServerUtils
import it.airgap.beaconsdk.internal.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class P2pClient(
    private val appName: String,
    private val serverUtils: P2pServerUtils,
    private val communicationUtils: P2pCommunicationUtils,
    private val matrixClients: List<MatrixClient>,
    private val replicationCount: Int,
    private val crypto: Crypto,
    private val keyPair: KeyPair
) {
    private val messageFlows: MutableMap<HexString, Flow<InternalResult<String>>> = mutableMapOf()
    private val keyPairs: MutableMap<HexString, SessionKeyPair> = mutableMapOf()

    private val handshakeInfo: P2pHandshakeInfo
        get() = P2pHandshakeInfo(
            appName,
            BeaconConfig.versionName,
            keyPair.publicKey.asHexString().value(withPrefix = false),
            serverUtils.getRelayServer(keyPair.publicKey)
        )

    private val handshakeInfoJson: String
        get() = Json.encodeToString(handshakeInfo)

    fun subscribeTo(publicKey: HexString): Flow<InternalResult<P2pMessage>> =
        messageFlows.getOrPut(publicKey) {
            matrixClients.map(MatrixClient::events)
                .merge() // @ExperimentalCoroutinesApi
                .onStart { startMatrixClients() }
                .filterIsInstance<MatrixEvent.TextMessage>()
                .filter { it.isTextMessageFrom(publicKey) }
                .map { decrypt(publicKey, it) }
        }.map { P2pMessage.fromInternalResult(publicKey, it) }

    suspend fun sendTo(publicKey: HexString, message: String): InternalResult<Unit> =
        tryResult {
            val encrypted = encrypt(publicKey, message).getOrThrow()
            for (i in 0 until replicationCount) {
                val relayServer = serverUtils.getRelayServer(publicKey, i.asHexString())
                val recipient = communicationUtils.getRecipientIdentifier(publicKey, relayServer)

                matrixClients.launch { it.sendTextMessageTo(recipient, encrypted) }
            }
        }

    suspend fun sendPairingRequest(
        publicKey: HexString,
        relayServer: String
    ): InternalResult<Unit> =
        tryResult {
            val recipient = communicationUtils.getRecipientIdentifier(publicKey, relayServer)
            val encrypted = crypto.encryptMessageWithPublicKey(handshakeInfoJson, publicKey.asByteArray()).getOrThrow()
            val message = communicationUtils.createOpenChannelMessage(recipient, encrypted)

            matrixClients.launch { it.sendTextMessageTo(recipient, message) }
        }

    private suspend fun startMatrixClients() {
        tryLog(TAG) {
            val loginDigest = crypto.hash(
                "login:${currentTimestamp() / 1000 / (5 * 60)}",
                32
            ).getOrThrow()

            val signature = crypto.signMessageDetached(loginDigest, keyPair.privateKey)
                .getOrThrow()
                .asHexString()
                .value(withPrefix = false)

            val publicKeyHex = keyPair.publicKey
                .asHexString()
                .value(withPrefix = false)

            val id = crypto.hashKey(keyPair.publicKey)
                .getOrThrow()
                .asHexString()
                .value(withPrefix = false)

            val password = "ed:$signature:${publicKeyHex}"
            val deviceId = publicKeyHex

            matrixClients.launch {
                it.start(id, password, deviceId)
            }
        }
    }

    private fun encrypt(publicKey: HexString, message: String): InternalResult<String> =
        keyPairs.getOrCreate(publicKey)
            .flatMap { crypto.encryptMessageWithSharedKey(message, it.tx) }

    private fun decrypt(
        publicKey: HexString,
        textMessage: MatrixEvent.TextMessage
    ): InternalResult<String> =
        keyPairs.getOrCreate(publicKey)
            .flatMap {
                crypto.decryptMessageWithSharedKey(
                    textMessage.message,
                    it.rx
                )
            }


    private fun MatrixEvent.isTextMessageFrom(publicKey: HexString): Boolean =
        this is MatrixEvent.TextMessage
                && communicationUtils.isMessageFrom(this, publicKey)
                && crypto.validateEncryptedMessage(message)

    private fun MutableMap<HexString, SessionKeyPair>.getOrCreate(publicKey: HexString): InternalResult<SessionKeyPair> =
        if (containsKey(publicKey)) internalSuccess(getValue(publicKey))
        else crypto.createServerSessionKeyPair(publicKey.asByteArray(), keyPair.privateKey)
            .alsoIfSuccess { put(publicKey, it) }

    private fun P2pMessage.Companion.fromInternalResult(
        publicKey: HexString,
        message: InternalResult<String>
    ): InternalResult<P2pMessage> =
        message.map { P2pMessage(publicKey.value(withPrefix = false), it) }

    private suspend fun MatrixClient.sendTextMessageTo(recipient: String, message: String) {
        val room = getRelevantRoom(recipient)
        room?.let { sendTextMessage(room, message) }
    }

    private suspend fun MatrixClient.getRelevantRoom(recipient: String): MatrixRoom? =
        joinedRooms()
            .firstOrNull { it.members.contains(recipient) }
            ?.also { logDebug(TAG, "Channel already open, reusing ${it.id}") }
            ?: run {
                logDebug(TAG, "No relevant rooms found")
                createTrustedPrivateRoom(recipient).getOrThrow()
            }

    companion object {
        const val TAG = "P2pCommunicationClient"
    }
}