package it.airgap.beaconsdk.core.internal.transport.p2p

import it.airgap.beaconsdk.core.data.beacon.P2pPeer
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.data.HexString
import it.airgap.beaconsdk.core.internal.transport.p2p.data.P2pMessage
import it.airgap.beaconsdk.core.internal.transport.p2p.matrix.MatrixClient
import it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data.MatrixEvent
import it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data.MatrixRoom
import it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data.api.MatrixError
import it.airgap.beaconsdk.core.internal.transport.p2p.store.*
import it.airgap.beaconsdk.core.internal.transport.p2p.utils.P2pCommunicator
import it.airgap.beaconsdk.core.internal.transport.p2p.utils.P2pCrypto
import it.airgap.beaconsdk.core.internal.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

public class P2pClient(
    private val matrixClient: MatrixClient,
    private val store: P2pStore,
    private val crypto: P2pCrypto,
    private val communicator: P2pCommunicator,
) {
    private val matrixEvents: Flow<MatrixEvent> by lazy {
        matrixClient.events
            .filter { event -> store.state().map { it.relayServer == event.node }.getOrDefault(true) }
            .onStart { tryLog(TAG) { matrixClient.start() } }
    }

    private val matrixMessageEvents: Flow<MatrixEvent.TextMessage> get() = matrixEvents.filterIsInstance()
    private val matrixInviteEvents: Flow<MatrixEvent.Invite> get() = matrixEvents.filterIsInstance()
    private val matrixJoinEvents: Flow<MatrixEvent.Join> get() = matrixEvents.filterIsInstance()

    private val subscribedFlows: MutableMap<HexString, MutableSet<String>> = mutableMapOf()

    private val startMutex: Mutex = Mutex()

    fun isSubscribed(publicKey: ByteArray): Boolean = subscribedFlows.containsKey(publicKey)

    fun subscribeTo(publicKey: ByteArray): Flow<Result<P2pMessage>> {
        val identifier = UUID.randomUUID().toString().also { subscribedFlows.addTo(publicKey, it) }

        return flow {
            try {
                matrixMessageEvents
                    .filter { it.isTextMessageFrom(publicKey) }
                    .onEach { store.intent(OnChannelEvent(it.sender, it.roomId)) }
                    .map { crypto.decrypt(publicKey, it.message) }
                    .map { P2pMessage.fromResult(publicKey, it) }
                    .collect {
                        if (subscribedFlows.containsElement(publicKey, identifier)) emit(it)
                        else cancelFlow()
                    }
            } catch (e: CancellationException) {
                /* no action */
            }
        }
    }

    suspend fun unsubscribeFrom(publicKey: ByteArray) {
        subscribedFlows.remove(publicKey)
        if (subscribedFlows.isEmpty()) {
            matrixClient.stop()
        }
    }

    suspend fun sendTo(peer: P2pPeer, message: String): Result<Unit> =
        runCatching {
            val publicKey = peer.publicKey.asHexString().toByteArray()
            val recipient = communicator.recipientIdentifier(publicKey, peer.relayServer).getOrThrow()
            val encrypted = crypto.encrypt(publicKey, message).getOrThrow()

            matrixClient.sendTextMessageTo(recipient.asString(), encrypted).getOrThrow()
        }

    suspend fun sendPairingResponse(peer: P2pPeer): Result<Unit> =
        runCatching {
            val publicKey = peer.publicKey.asHexString().toByteArray()
            val recipient = communicator.recipientIdentifier(publicKey, peer.relayServer).getOrThrow()
            val relayServer = store.state().getOrThrow().relayServer
            val payload = crypto.encryptPairingPayload(
                publicKey,
                communicator.pairingPayload(peer, relayServer).getOrThrow(),
            ).getOrThrow()
            val message = communicator.channelOpeningMessage(recipient.asString(), payload.toHexString().asString())

            matrixClient.sendTextMessageTo(recipient.asString(), message, newRoom = true).getOrThrow()
        }

    private fun MatrixEvent.isTextMessageFrom(publicKey: ByteArray): Boolean =
        this is MatrixEvent.TextMessage
                && communicator.isMessageFrom(this, publicKey)
                && communicator.isValidMessage(this)

    private fun P2pMessage.Companion.fromResult(
        publicKey: ByteArray,
        message: Result<String>,
    ): Result<P2pMessage> =
        message.map { P2pMessage(publicKey.toHexString().asString(), it) }

    private suspend fun MatrixClient.start() {
        startMutex.withLock withLock@ {
            if (isLoggedIn()) return@withLock

            val id = crypto.userId().getOrThrow()
            val deviceId = crypto.deviceId().getOrThrow()

            val startRetries = store.state().getOrThrow().availableNodes
            runCatchingFlatRepeat(startRetries) {
                val relayServer = store.state().getOrThrow().relayServer
                val password = crypto.password().getOrThrow()

                start(relayServer, id, password, deviceId).onFailure { this@P2pClient.resetHard() }
            }.getOrThrow()

            CoroutineScope(CoroutineName("collectInviteEvents")).launch {
                matrixInviteEvents
                    .distinctUntilChangedBy { it.roomId }
                    .collect {
                        store.intent(OnChannelEvent(it.sender, it.roomId))
                        tryLog(TAG) {
                            joinRoomRepeated(it.roomId)
                        }
                    }
            }
        }
    }

    private suspend fun resetHard() {
        store.intent(HardReset)
        matrixClient.resetHard()
    }

    private tailrec suspend fun MatrixClient.joinRoomRepeated(
        roomId: String,
        retry: Int = BeaconConfiguration.P2P_MAX_JOIN_RETRIES,
        carryError: Throwable? = null,
    ) {
        if (retry < 0) carryError?.let { throw it } ?: failWithJoiningRoomFailed(roomId)
        logDebug(TAG, "Joining room $roomId (retries remaining: $retry)")

        val relayServer = store.state().getOrThrow().relayServer
        val error = joinRoom(relayServer, roomId).exceptionOrNull() ?: return
        if (error !is MatrixError.Forbidden) throw error

        // Joining a room too quickly after receiving an invite event
        // sometimes results in rejection from the server in a federated multi-node setup.
        // Usually waiting a few milliseconds solves the issue.

        delay(BeaconConfiguration.P2P_JOIN_DELAY_MS)
        return joinRoomRepeated(roomId, retry - 1, error)
    }

    private suspend fun MatrixClient.sendTextMessageTo(
        recipient: String,
        message: String,
        newRoom: Boolean = false,
    ): Result<Unit> =
        runCatching runCatching@ {
            start()

            val roomId = (
                if (newRoom) createRoom(recipient).getOrThrow()?.id
                else roomId(recipient).getOrThrow()
            ) ?: failWithRoomNotFound(recipient)

            val relayServer = store.state().getOrThrow().relayServer

            val error = sendTextMessage(relayServer, roomId, message).exceptionOrNull() ?: return@runCatching
            if (error !is MatrixError.Forbidden) throw error

            store.intent(OnChannelClosed(roomId))

            val newRoomId = roomId(recipient).getOrThrow() ?: failWithRoomNotFound(recipient)
            sendTextMessage(relayServer, newRoomId, message).getOrThrow()
        }

    private suspend fun MatrixClient.roomId(recipient: String): Result<String?> =
        runCatching runCatching@ {
            start()

            val activeChannels = store.state().getOrNull()?.activeChannels
            activeChannels?.get(recipient)?.let {
                return@runCatching it
            }

            val openChannel = joinedRooms().firstOrNull { it.isActive() && it.hasMember(recipient) }
            openChannel?.let {
                logDebug(TAG, "Channel already open, reusing ${it.id}")
                return@runCatching it.id
            }

            logDebug(TAG, "No relevant rooms found")
            createRoom(recipient).getOrThrow()?.id
        }

    private suspend fun MatrixClient.createRoom(member: String): Result<MatrixRoom?> =
        runCatching {
            val relayServer = store.state().getOrThrow().relayServer
            val room = createTrustedPrivateRoom(relayServer, member).getOrThrow()

            room?.also {
                it.waitForMember(member)
                store.intent(OnChannelCreated(member, it.id))
            }
        }

    private suspend fun MatrixRoom.isActive(recipient: String): Boolean {
        val activeChannels = store.state().getOrNull()?.activeChannels ?: return false
        return activeChannels[recipient] == id
    }

    private suspend fun MatrixRoom.isActive(): Boolean = !isInactive()
    private suspend fun MatrixRoom.isInactive(): Boolean {
        val inactiveChannels = store.state().getOrNull()?.inactiveChannels ?: return false
        return inactiveChannels.contains(id)
    }

    private suspend fun MatrixRoom.waitForMember(member: String) {
        if (hasMember(member)) return
        logDebug(TAG, "Waiting for $member to join room $id")
        matrixJoinEvents.first { it.roomId == id && it.userId == member }
        logDebug(TAG, "$member joined room $id")
    }

    private fun <V> MutableMap<HexString, V>.containsKey(key: ByteArray) = containsKey(key.toHexString())
    private fun <V> MutableMap<HexString, V>.remove(key: ByteArray): V? = remove(key.toHexString())

    private fun <V> MutableMap<HexString, MutableSet<V>>.addTo(key: ByteArray, value: V) {
        getOrPut(key.toHexString()) { mutableSetOf() }.add(value)
    }

    private fun <V> MutableMap<HexString, MutableSet<V>>.containsElement(key: ByteArray, value: V): Boolean =
        get(key.toHexString())?.contains(value) ?: false

    private fun cancelFlow(): Nothing = throw CancellationException()

    private fun failWithJoiningRoomFailed(roomId: String): Nothing = failWith("Failed to join room $roomId.")
    private fun failWithRoomNotFound(member: String): Nothing = failWith("Failed to find room for member $member.")

    companion object {
        const val TAG = "P2pClient"
    }
}