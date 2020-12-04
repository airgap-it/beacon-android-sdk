package it.airgap.beaconsdk.internal.transport.p2p

import it.airgap.beaconsdk.data.beacon.*
import it.airgap.beaconsdk.internal.message.ConnectionMessage
import it.airgap.beaconsdk.internal.message.ConnectionTransportMessage
import it.airgap.beaconsdk.internal.message.SerializedConnectionMessage
import it.airgap.beaconsdk.internal.storage.manager.StorageManager
import it.airgap.beaconsdk.internal.transport.Transport
import it.airgap.beaconsdk.internal.transport.p2p.data.P2pMessage
import it.airgap.beaconsdk.internal.utils.HexString
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.Success
import it.airgap.beaconsdk.internal.utils.tryResult
import kotlinx.coroutines.flow.*

internal class P2pTransport(
    private val storageManager: StorageManager,
    private val client: P2pClient,
) : Transport() {
    override val type: Connection.Type = Connection.Type.P2P

    override val connectionMessages: Flow<InternalResult<ConnectionTransportMessage>> by lazy {
        storageManager.updatedPeers
            .filterIsInstance<P2pPeer>()
            .onEach { onUpdatedP2pPeer(it) }
            .filterNot { it.isRemoved }
            .mapNotNull { HexString.fromStringOrNull(it.publicKey) }
            .filterNot { client.isSubscribed(it) }
            .flatMapMerge { subscribeToP2pPeer(it) }
            .map { ConnectionMessage.fromInternalResult(it) }
    }

    override suspend fun sendMessage(message: ConnectionTransportMessage): InternalResult<Unit> =
        when (message) {
            is SerializedConnectionMessage -> sendSerializedMessage(message.content, message.origin.id)
            else -> Success()
        }

    private suspend fun sendSerializedMessage(message: String, recipient: String): InternalResult<Unit> =
        tryResult { client.sendTo(HexString.fromString(recipient), message).get() }

    private suspend fun onUpdatedP2pPeer(peer: P2pPeer) {
        if (!peer.isPaired && !peer.isRemoved) pairP2pPeer(peer)
        if (peer.isRemoved) unsubscribeFromP2pPeer(peer)
    }

    private suspend fun pairP2pPeer(peer: P2pPeer) {
        val result = client.sendPairingResponse(peer)

        if (result.isSuccess) {
            storageManager.updatePeers(
                listOf(peer.selfPaired()),
                compareWith = listOf(
                    P2pPeer::id,
                    P2pPeer::name,
                    P2pPeer::publicKey,
                    P2pPeer::relayServer,
                    P2pPeer::icon,
                    P2pPeer::appUrl,
                )
            )
        }
    }

    private fun subscribeToP2pPeer(publicKey: HexString): Flow<InternalResult<P2pMessage>> =
        client.subscribeTo(publicKey)

    private fun unsubscribeFromP2pPeer(peerInfo: Peer) {
        val publicKey = HexString.fromString(peerInfo.publicKey)
        client.unsubscribeFrom(publicKey)
    }

    private fun ConnectionMessage.Companion.fromInternalResult(
        p2pMessage: InternalResult<P2pMessage>,
    ): InternalResult<ConnectionTransportMessage> = p2pMessage.map { SerializedConnectionMessage(Origin.P2P(it.id), it.content) }
}