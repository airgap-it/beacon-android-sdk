package it.airgap.beaconsdk.core.internal.transport.p2p

import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.data.P2pPeer
import it.airgap.beaconsdk.core.data.selfPaired
import it.airgap.beaconsdk.core.internal.message.*
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.transport.Transport
import it.airgap.beaconsdk.core.internal.transport.p2p.data.P2pMessage
import it.airgap.beaconsdk.core.internal.transport.p2p.store.DiscardPairingData
import it.airgap.beaconsdk.core.internal.transport.p2p.store.OnPairingCompleted
import it.airgap.beaconsdk.core.internal.transport.p2p.store.OnPairingRequested
import it.airgap.beaconsdk.core.internal.transport.p2p.store.P2pTransportStore
import it.airgap.beaconsdk.core.internal.utils.flatMap
import it.airgap.beaconsdk.core.internal.utils.runCatchingFlat
import it.airgap.beaconsdk.core.internal.utils.success
import it.airgap.beaconsdk.core.storage.findPeer
import it.airgap.beaconsdk.core.transport.data.*
import it.airgap.beaconsdk.core.transport.p2p.P2pClient
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class)
internal class P2pTransport(
    private val storageManager: StorageManager,
    private val client: P2pClient,
    private val store: P2pTransportStore,
) : Transport() {
    override val type: Connection.Type = Connection.Type.P2P

    override val incomingConnectionMessages: Flow<Result<IncomingConnectionTransportMessage>> by lazy {
        storageManager.updatedPeers
            .filterIsInstance<P2pPeer>()
            .onEach { onUpdatedP2pPeer(it) }
            .filterNot { it.isRemoved || client.isSubscribed(it) }
            .mapNotNull { client.subscribeTo(it) }
            .flattenMerge()
            .map { IncomingConnectionMessage.fromResult(it) }
    }

    override suspend fun pair(): Flow<Result<PairingMessage>> = flow {
        val pairingRequest = client.createPairingRequest().also {
            emit(it)
        }.getOrNull() ?: return@flow

        store.intent(OnPairingRequested)

        client.pairingResponses
            .filter { it.isFailure || it.getOrNull()?.id == pairingRequest.id }
            .onEach { result -> result.getOrNull()?.let { addPeer(it) } }
            .collect { emit(it) }
    }

    override suspend fun pair(request: PairingRequest): Result<PairingResponse> =
        runCatchingFlat {
            when (request) {
                is P2pPairingRequest -> runCatching { addPeer(request) }.flatMap { client.createPairingResponse(request) }
            }
        }

    override fun supportsPairing(request: PairingRequest): Boolean = request is P2pPairingRequest

    override suspend fun sendMessage(message: OutgoingConnectionTransportMessage): Result<Unit> =
        runCatchingFlat {
            val peerPublicKey = message.destination?.id
            val peer = storageManager.findPeer<P2pPeer> { it.publicKey == peerPublicKey }
                ?: store.state().getOrThrow().pairingPeerDeferred?.await()?.also { store.intent(DiscardPairingData) }
                ?: failWithUnknownPeer(peerPublicKey)

            return when (message) {
                is SerializedOutgoingConnectionMessage -> sendSerializedMessage(message.content, peer)
                else -> Result.success()
            }
        }

    private suspend fun sendSerializedMessage(
        message: String,
        recipient: P2pPeer,
    ): Result<Unit> = client.sendTo(recipient, message)

    private suspend fun addPeer(pairingData: P2pPairingMessage) {
        when (val peer = pairingData.toPeer()) {
            is P2pPeer -> {
                storageManager.addPeers(listOf(peer), overwrite = true) { listOfNotNull(id) }
                store.intent(OnPairingCompleted(peer))
            }
        }
    }

    private suspend fun onUpdatedP2pPeer(peer: P2pPeer) {
        if (!peer.isPaired && !peer.isRemoved) pairP2pPeer(peer)
        if (peer.isRemoved) client.unsubscribeFrom(peer)
    }

    private suspend fun pairP2pPeer(peer: P2pPeer) {
        val result = client.sendPairingResponse(peer)

        if (result.isSuccess) {
            storageManager.updatePeers(listOf(peer.selfPaired())) { listOfNotNull(id, name, publicKey, relayServer, icon, appUrl) }
        }
    }

    private fun failWithUnknownPeer(publicKey: String?): Nothing =
        throw IllegalStateException("P2P peer with public key $publicKey is not recognized.")

    private fun IncomingConnectionMessage.Companion.fromResult(
        p2pMessage: Result<P2pMessage>,
    ): Result<IncomingConnectionTransportMessage> =
        p2pMessage.map { SerializedIncomingConnectionMessage(Connection.Id.P2P(it.publicKey), it.content) }
}