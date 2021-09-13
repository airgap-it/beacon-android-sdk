package it.airgap.beaconsdk.transport.p2p.matrix

import it.airgap.beaconsdk.core.data.beacon.P2pPeer
import it.airgap.beaconsdk.core.internal.data.HexString
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.transport.p2p.P2pClient
import it.airgap.beaconsdk.core.internal.transport.p2p.data.P2pMessage
import it.airgap.beaconsdk.core.internal.utils.*
import it.airgap.beaconsdk.core.internal.utils.delegate.default
import it.airgap.beaconsdk.core.network.provider.HttpProvider
import it.airgap.beaconsdk.transport.p2p.matrix.data.MatrixRoom
import it.airgap.beaconsdk.transport.p2p.matrix.internal.BeaconP2pMatrixConfiguration
import it.airgap.beaconsdk.transport.p2p.matrix.internal.P2pCommunicator
import it.airgap.beaconsdk.transport.p2p.matrix.internal.P2pCrypto
import it.airgap.beaconsdk.transport.p2p.matrix.internal.di.ExtendedDependencyRegistry
import it.airgap.beaconsdk.transport.p2p.matrix.internal.di.extend
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.MatrixClient
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.MatrixEvent
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.MatrixError
import it.airgap.beaconsdk.transport.p2p.matrix.internal.storage.sharedpreferences.SharedPreferencesMatrixStoragePlugin
import it.airgap.beaconsdk.transport.p2p.matrix.internal.store.*
import it.airgap.beaconsdk.transport.p2p.matrix.storage.P2pMatrixStoragePlugin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

/**
 * Beacon P2P implementation that uses [Matrix](https://matrix.org/) network for the communication.
 */
public class P2pMatrix internal constructor(
    private val matrix: MatrixClient,
    private val store: P2pStore,
    private val crypto: P2pCrypto,
    private val communicator: P2pCommunicator,
) : P2pClient {
    private val matrixEvents: Flow<MatrixEvent> by lazy {
        matrix.events
            .filter { event -> store.state().map { it.relayServer == event.node }.getOrDefault(true) }
            .onStart { tryLog(TAG) { matrix.start() } }
    }

    private val matrixMessageEvents: Flow<MatrixEvent.TextMessage> get() = matrixEvents.filterIsInstance()
    private val matrixInviteEvents: Flow<MatrixEvent.Invite> get() = matrixEvents.filterIsInstance()
    private val matrixJoinEvents: Flow<MatrixEvent.Join> get() = matrixEvents.filterIsInstance()

    private val subscribedFlows: MutableMap<HexString, MutableSet<String>> = mutableMapOf()

    private val startMutex: Mutex = Mutex()

    /**
     * Checks if [peer] has been already subscribed.
     */
    override fun isSubscribed(peer: P2pPeer): Boolean {
        val publicKey = peer.publicKey.asHexStringOrNull() ?: return false

        return subscribedFlows.containsKey(publicKey)
    }

    /**
     * Subscribes to a [peer] returning a [Flow] of received [P2pMessage]
     * instances identified as sent by the subscribed peer or occurred errors represented as a [Result].
     */
    override fun subscribeTo(peer: P2pPeer): Flow<Result<P2pMessage>>? {
        val publicKey = peer.publicKey.asHexStringOrNull()?.toByteArray() ?: return null
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

    /**
     * Removes an active subscription for the [peer].
     */
    override suspend fun unsubscribeFrom(peer: P2pPeer) {
        val publicKey = peer.publicKey.asHexStringOrNull() ?: return

        subscribedFlows.remove(publicKey)
        if (subscribedFlows.isEmpty()) {
            matrix.stop()
        }
    }

    /**
     * Sends a text message to the specified [peer].
     */
    override suspend fun sendTo(peer: P2pPeer, message: String): Result<Unit> =
        runCatching {
            val publicKey = peer.publicKey.asHexString().toByteArray()
            val recipient = communicator.recipientIdentifier(publicKey, peer.relayServer).getOrThrow()
            val encrypted = crypto.encrypt(publicKey, message).getOrThrow()

            matrix.sendTextMessageTo(recipient.asString(), encrypted).getOrThrow()
        }

    /**
     * Sends a pairing message to the specified [peer].
     */
    override suspend fun sendPairingResponse(peer: P2pPeer): Result<Unit> =
        runCatching {
            val publicKey = peer.publicKey.asHexString().toByteArray()
            val recipient = communicator.recipientIdentifier(publicKey, peer.relayServer).getOrThrow()
            val relayServer = store.state().getOrThrow().relayServer
            val payload = crypto.encryptPairingPayload(
                publicKey,
                communicator.pairingPayload(peer, relayServer).getOrThrow(),
            ).getOrThrow()
            val message = communicator.channelOpeningMessage(recipient.asString(), payload.toHexString().asString())

            matrix.sendTextMessageTo(recipient.asString(), message, newRoom = true).getOrThrow()
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

                start(relayServer, id, password, deviceId).onFailure { this@P2pMatrix.resetHard() }
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
        matrix.resetHard()
    }

    private tailrec suspend fun MatrixClient.joinRoomRepeated(
        roomId: String,
        retry: Int = BeaconP2pMatrixConfiguration.MAX_JOIN_RETRIES,
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

        delay(BeaconP2pMatrixConfiguration.JOIN_DELAY_MS)
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

    private fun <V> MutableMap<HexString, MutableSet<V>>.addTo(key: ByteArray, value: V) {
        getOrPut(key.toHexString()) { mutableSetOf() }.add(value)
    }

    private fun <V> MutableMap<HexString, MutableSet<V>>.containsElement(key: ByteArray, value: V): Boolean =
        get(key.toHexString())?.contains(value) ?: false

    private fun cancelFlow(): Nothing = throw CancellationException()

    private fun failWithJoiningRoomFailed(roomId: String): Nothing = failWith("Failed to join room $roomId.")
    private fun failWithRoomNotFound(member: String): Nothing = failWith("Failed to find room for member $member.")

    /**
     * Factory for [P2pMatrix].
     *
     * @constructor Creates a factory needed for dynamic [P2pMatrix] registration.
     *
     * @property [storagePlugin] An optional external implementation of [P2pMatrixStoragePlugin]. If not provided, an internal implementation will be used.
     * @property [matrixNodes] A list of Matrix nodes used in the connection, set to [BeaconP2pMatrixConfiguration.defaultNodes] by default.
     * One node will be selected randomly based on the local key pair and used as the primary connection node,
     * the rest will be used as a fallback if the primary node goes down.
     * @property [httpProvider] An optional external [HttpProvider] implementation used to make Beacon HTTP requests.
     */
    public class Factory(
        storagePlugin: P2pMatrixStoragePlugin? = null,
        private val matrixNodes: List<String> = BeaconP2pMatrixConfiguration.defaultNodes,
        private val httpProvider: HttpProvider? = null
    ) : P2pClient.Factory<P2pMatrix> {
        private var _extendedDependencyRegistry: ExtendedDependencyRegistry? = null
        private fun extendedDependencyRegistry(dependencyRegistry: DependencyRegistry): ExtendedDependencyRegistry =
            _extendedDependencyRegistry ?: dependencyRegistry.extend().also { _extendedDependencyRegistry = it }

        private var storagePlugin: P2pMatrixStoragePlugin by default(storagePlugin) { SharedPreferencesMatrixStoragePlugin.create(applicationContext) }

        override fun create(dependencyRegistry: DependencyRegistry): P2pMatrix =
            with(extendedDependencyRegistry(dependencyRegistry)) {
                with(storageManager) {
                    if (!hasPlugin<P2pMatrixStoragePlugin>()) addPlugins(storagePlugin.extend())
                }

                P2pMatrix(matrixClient(httpProvider), p2pStore(httpProvider, matrixNodes), p2pCrypto, p2pCommunicator)
            }
    }

    public companion object {
        private const val TAG = "P2pMatrix"
    }
}

/**
 * Creates a new instance of [P2pMatrix.Factory] configured with optional [storagePlugin], [matrixNodes] and [httpProvider].
 */
public fun p2pMatrix(
    storagePlugin: P2pMatrixStoragePlugin? = null,
    matrixNodes: List<String> = BeaconP2pMatrixConfiguration.defaultNodes,
    httpProvider: HttpProvider? = null,
): P2pMatrix.Factory = P2pMatrix.Factory(storagePlugin, matrixNodes, httpProvider)