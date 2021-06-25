package it.airgap.beaconsdk.internal.transport.p2p

import it.airgap.beaconsdk.data.beacon.P2pPeer
import it.airgap.beaconsdk.internal.BeaconConfiguration
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.internal.crypto.data.SessionKeyPair
import it.airgap.beaconsdk.internal.transport.p2p.data.P2pMessage
import it.airgap.beaconsdk.internal.transport.p2p.matrix.MatrixClient
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixEvent
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixRoom
import it.airgap.beaconsdk.internal.transport.p2p.utils.P2pCommunicationUtils
import it.airgap.beaconsdk.internal.transport.p2p.utils.P2pServerUtils
import it.airgap.beaconsdk.internal.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*

internal class P2pClient(
    private val appName: String,
    private val appIcon: String?,
    private val appUrl: String?,
    private val serverUtils: P2pServerUtils,
    private val communicationUtils: P2pCommunicationUtils,
    private val matrixClients: List<MatrixClient>,
    private val crypto: Crypto,
    private val keyPair: KeyPair,
) {
    private val subscribedFlows: MutableMap<HexString, MutableSet<String>> = mutableMapOf()

    private val serverSessionKeyPair: MutableMap<HexString, SessionKeyPair> = mutableMapOf()
    private val clientSessionKeyPair: MutableMap<HexString, SessionKeyPair> = mutableMapOf()

    private val matrixEvents: Flow<MatrixEvent> by lazy {
        matrixClients
            .map(MatrixClient::events)
            .merge()
            .onStart { matrixClients.filterNot { it.isLoggedIn() }.launch { it.start() } }
    }

    private val matrixMessageEvents: Flow<MatrixEvent.TextMessage> get() = matrixEvents.filterIsInstance()
    private val matrixInviteEvents: Flow<MatrixEvent.Invite> get() = matrixEvents.filterIsInstance()
    private val matrixJoinEvents: Flow<MatrixEvent.Join> get() = matrixEvents.filterIsInstance()

    fun isSubscribed(publicKey: HexString): Boolean = subscribedFlows.containsKey(publicKey)

    fun subscribeTo(publicKey: HexString): Flow<InternalResult<P2pMessage>> {
        val identifier = UUID.randomUUID().toString().also { subscribedFlows.addTo(publicKey, it) }

        return flow {
            try {
                matrixMessageEvents
                    .filter { it.isTextMessageFrom(publicKey) }
                    .map { decrypt(publicKey, it) }
                    .map { P2pMessage.fromInternalResult(publicKey, it) }
                    .collect {
                        if (subscribedFlows.containsElement(publicKey, identifier)) emit(it)
                        else cancelFlow()
                    }
            } catch (e: CancellationException) {
                /* no action */
            }
        }
    }

    fun unsubscribeFrom(publicKey: HexString) {
        subscribedFlows.remove(publicKey)
    }

    suspend fun sendTo(peer: P2pPeer, message: String): InternalResult<Unit> =
        tryResult {
            val publicKey = HexString.fromString(peer.publicKey)
            val recipient = communicationUtils.recipientIdentifier(publicKey, peer.relayServer)
            val encrypted = encrypt(publicKey, message).get().asHexString()

            matrixClients.launch { it.sendTextMessageTo(recipient, encrypted.asString()).get() }
        }

    suspend fun sendPairingResponse(peer: P2pPeer): InternalResult<Unit> =
        tryResult {
            val publicKey = HexString.fromString(peer.publicKey)
            val recipient = communicationUtils.recipientIdentifier(publicKey, peer.relayServer)
            val payload = crypto.encryptMessageWithPublicKey(
                communicationUtils.pairingPayload(
                    peer,
                    appName,
                    appIcon,
                    appUrl,
                    keyPair.publicKey,
                    serverUtils.getRelayServer(keyPair.publicKey)
                ).get(),
                publicKey.asByteArray()
            ).get().asHexString()
            val message = communicationUtils.channelOpeningMessage(recipient, payload.asString())

            matrixClients.launch { it.sendTextMessageTo(recipient, message).get() }
        }

    private fun encrypt(publicKey: HexString, message: String): InternalResult<ByteArray> =
        getOrCreateClientSessionKeyPair(publicKey)
            .flatMap { crypto.encryptMessageWithSharedKey(message, it.tx) }

    private fun decrypt(
        publicKey: HexString,
        textMessage: MatrixEvent.TextMessage,
    ): InternalResult<String> =
        getOrCreateServerSessionKeyPair(publicKey)
            .flatMap {
                if (textMessage.message.isHex()) crypto.decryptMessageWithSharedKey(HexString.fromString(
                    textMessage.message), it.rx)
                else crypto.decryptMessageWithSharedKey(textMessage.message, it.rx)
            }
            .map { it.decodeToString() }

    private fun MatrixEvent.isTextMessageFrom(publicKey: HexString): Boolean =
        this is MatrixEvent.TextMessage
                && communicationUtils.isMessageFrom(this, publicKey)
                && crypto.validateEncryptedMessage(message)

    private fun getOrCreateClientSessionKeyPair(publicKey: HexString): InternalResult<SessionKeyPair> =
        clientSessionKeyPair.getOrCreate(publicKey) {
            crypto.createClientSessionKeyPair(publicKey.asByteArray(), keyPair.privateKey)
        }

    private fun getOrCreateServerSessionKeyPair(publicKey: HexString): InternalResult<SessionKeyPair> =
        serverSessionKeyPair.getOrCreate(publicKey) {
            crypto.createServerSessionKeyPair(publicKey.asByteArray(), keyPair.privateKey)
        }

    private fun MutableMap<HexString, SessionKeyPair>.getOrCreate(
        publicKey: HexString,
        default: () -> InternalResult<SessionKeyPair>,
    ): InternalResult<SessionKeyPair> =
        if (containsKey(publicKey)) Success(getValue(publicKey))
        else default().alsoIfSuccess { put(publicKey, it) }

    private fun P2pMessage.Companion.fromInternalResult(
        publicKey: HexString,
        message: InternalResult<String>,
    ): InternalResult<P2pMessage> =
        message.map { P2pMessage(publicKey.asString(), it) }

    private suspend fun MatrixClient.start() {
        if (isLoggedIn()) return

        tryLog(TAG) {
            val loginDigest = crypto.hash(
                "login:${currentTimestamp() / 1000 / (5 * 60)}",
                32
            ).get()

            val signature = crypto.signMessageDetached(loginDigest, keyPair.privateKey)
                .get()
                .asHexString()
                .asString()

            val publicKeyHex = keyPair.publicKey
                .asHexString()
                .asString()

            val id = crypto.hashKey(keyPair.publicKey)
                .get()
                .asHexString()
                .asString()

            val password = "ed:$signature:${publicKeyHex}"
            val deviceId = publicKeyHex

            start(id, password, deviceId)

            CoroutineScope(CoroutineName("collectInviteEvents") + Dispatchers.Default).launch {
                matrixInviteEvents.collect {
                    joinRoomsRepeated(it.roomId)
                }
            }
        }
    }

    private tailrec suspend fun MatrixClient.joinRoomsRepeated(roomId: String, retry: Int = BeaconConfiguration.P2P_MAX_JOIN_RETRIES) {
        if (retry < 0) return
        logDebug(TAG, "Joining room $roomId (retries remaining: $retry)")

        val error = joinRoom(roomId).errorOrNull() ?: return
        // TODO: check error type before continuing

        logDebug(TAG, "Joining room $roomId failed with error $error")

        delay(BeaconConfiguration.P2P_JOIN_DELAY_MS)
        return joinRoomsRepeated(roomId, retry - 1)
    }

    private suspend fun MatrixClient.sendTextMessageTo(
        recipient: String,
        message: String,
    ): InternalResult<Unit> =
        tryResult {
            start()

            val room = getRelevantRoom(recipient).get()
            room?.let { sendTextMessage(it, message).get() }
        }

    private suspend fun MatrixClient.getRelevantRoom(recipient: String): InternalResult<MatrixRoom?> {
        start()

        return joinedRooms()
            .firstOrNull { it.members.contains(recipient) }
            ?.let {
                logDebug(TAG, "Channel already open, reusing ${it.id}")
                Success(it)
            } ?: run {
            logDebug(TAG, "No relevant rooms found")
            createTrustedPrivateRoom(recipient).mapSuspend { room ->
                room?.also { waitForMember(it, recipient) }
            }
        }
    }

    private suspend fun waitForMember(room: MatrixRoom, member: String) {
        if (room.members.contains(member)) return
        logDebug(TAG, "Waiting for $member to join room ${room.id}")
        matrixJoinEvents.first { it.roomId == room.id && it.userId == member }
        logDebug(TAG, "$member joined room ${room.id}")
    }

    private fun <K, V> MutableMap<K, MutableSet<V>>.addTo(key: K, value: V) {
        getOrPut(key) { mutableSetOf() }.add(value)
    }

    private fun <K, V> MutableMap<K, MutableSet<V>>.containsElement(key: K, value: V): Boolean =
        get(key)?.contains(value) ?: false

    private fun cancelFlow(): Nothing = throw CancellationException()

    companion object {
        const val TAG = "P2pClient"
    }
}