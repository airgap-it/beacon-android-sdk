package it.airgap.beaconsdk.core.internal.transport.p2p

import it.airgap.beaconsdk.core.data.beacon.Connection
import it.airgap.beaconsdk.core.data.beacon.Origin
import it.airgap.beaconsdk.core.data.beacon.P2pPeer
import it.airgap.beaconsdk.core.data.beacon.selfPaired
import it.airgap.beaconsdk.core.internal.message.ConnectionMessage
import it.airgap.beaconsdk.core.internal.message.ConnectionTransportMessage
import it.airgap.beaconsdk.core.internal.message.SerializedConnectionMessage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.transport.Transport
import it.airgap.beaconsdk.core.internal.transport.p2p.data.P2pMessage
import it.airgap.beaconsdk.core.internal.utils.runCatchingFlat
import it.airgap.beaconsdk.core.internal.utils.success
import it.airgap.beaconsdk.core.transport.p2p.P2pClient
import kotlinx.coroutines.flow.*

internal class P2pTransport(
    private val storageManager: StorageManager,
    private val client: P2pClient,
) : Transport() {
    override val type: Connection.Type = Connection.Type.P2P

    override val connectionMessages: Flow<Result<ConnectionTransportMessage>> by lazy {
        storageManager.updatedPeers
            .filterIsInstance<P2pPeer>()
            .onEach { onUpdatedP2pPeer(it) }
            .filterNot { it.isRemoved || client.isSubscribed(it) }
            .mapNotNull { client.subscribeTo(it) }
            .flattenMerge()
            .map { ConnectionMessage.fromResult(it) }
    }

    override suspend fun sendMessage(message: ConnectionTransportMessage): Result<Unit> =
        runCatchingFlat {
            val peerPublicKey = message.origin.id
            val peer = storageManager.findInstancePeer<P2pPeer> { it.publicKey == peerPublicKey } ?: failWithUnknownPeer(peerPublicKey)

            return when (message) {
                is SerializedConnectionMessage -> sendSerializedMessage(message.content, peer)
                else -> Result.success()
            }
        }

    private suspend fun sendSerializedMessage(
        message: String,
        recipient: P2pPeer,
    ): Result<Unit> = client.sendTo(recipient, message)

    private suspend fun onUpdatedP2pPeer(peer: P2pPeer) {
        if (!peer.isPaired && !peer.isRemoved) pairP2pPeer(peer)
        if (peer.isRemoved) client.unsubscribeFrom(peer)
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

    private fun failWithUnknownPeer(publicKey: String): Nothing =
        throw IllegalStateException("P2P peer with public key $publicKey is not recognized.")

    private fun ConnectionMessage.Companion.fromResult(
        p2pMessage: Result<P2pMessage>,
    ): Result<ConnectionTransportMessage> =
        p2pMessage.map { SerializedConnectionMessage(Origin.P2P(it.id), it.content) }
}