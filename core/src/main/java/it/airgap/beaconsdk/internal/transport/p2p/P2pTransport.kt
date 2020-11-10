package it.airgap.beaconsdk.internal.transport.p2p

import it.airgap.beaconsdk.data.beacon.Origin
import it.airgap.beaconsdk.data.beacon.P2pPeerInfo
import it.airgap.beaconsdk.internal.message.ConnectionMessage
import it.airgap.beaconsdk.internal.message.ConnectionTransportMessage
import it.airgap.beaconsdk.internal.message.SerializedConnectionMessage
import it.airgap.beaconsdk.internal.storage.decorator.DecoratedExtendedStorage
import it.airgap.beaconsdk.internal.transport.Transport
import it.airgap.beaconsdk.internal.transport.p2p.data.P2pMessage
import it.airgap.beaconsdk.internal.utils.HexString
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.launch
import it.airgap.beaconsdk.internal.utils.tryResult
import kotlinx.coroutines.flow.*

internal class P2pTransport(
    private val storage: DecoratedExtendedStorage,
    private val client: P2pClient,
) : Transport() {
    override val type: Type = Type.P2P

    override val connectionMessages: Flow<InternalResult<ConnectionTransportMessage>> by lazy {
        storage.updatedP2pPeers
            .onEach { onUpdatedP2pPeer(it) }
            .filterNot { it.isRemoved }
            .mapNotNull { HexString.fromStringOrNull(it.publicKey) }
            .filterNot { client.isSubscribed(it) }
            .flatMapMerge { subscribeToP2pPeer(it) }
            .map { ConnectionMessage.fromInternalResult(it) }
    }

    override suspend fun sendMessage(message: String, recipient: String?): InternalResult<Unit> =
        tryResult {
            val knownPeers = storage.getP2pPeers()
            val recipients = recipient
                ?.let { knownPeers.filterWithPublicKey(it).ifEmpty { failWithUnknownRecipient() } }
                ?: knownPeers

            recipients.launch {
                client.sendTo(HexString.fromString(it.publicKey), message).value()
            }
        }

    private suspend fun onUpdatedP2pPeer(peerInfo: P2pPeerInfo) {
        if (!peerInfo.isPaired && !peerInfo.isRemoved) pairP2pPeer(peerInfo)
        if (peerInfo.isRemoved) unsubscribeFromP2pPeer(peerInfo)
    }

    private suspend fun pairP2pPeer(peerInfo: P2pPeerInfo) {
        val result = with(peerInfo)
        {
            client.sendPairingRequest(
                HexString.fromString(publicKey),
                relayServer,
                version
            )
        }

        if (result.isSuccess) {
            storage.addP2pPeers(listOf(peerInfo.copy(isPaired = true)), overwrite = true) { a, b ->
                a.name == b.name
                        && a.publicKey == b.publicKey
                        && a.relayServer == b.relayServer
                        && a.icon == b.icon
                        && a.appUrl == b.appUrl
            }
        }
    }

    private fun subscribeToP2pPeer(publicKey: HexString): Flow<InternalResult<P2pMessage>> =
        client.subscribeTo(publicKey)

    private fun unsubscribeFromP2pPeer(peerInfo: P2pPeerInfo) {
        val publicKey = HexString.fromString(peerInfo.publicKey)
        client.unsubscribeFrom(publicKey)
    }

    private fun ConnectionMessage.Companion.fromInternalResult(
        p2pMessage: InternalResult<P2pMessage>,
    ): InternalResult<ConnectionTransportMessage> = p2pMessage.map { SerializedConnectionMessage(Origin.P2P(it.id), it.content) }

    private fun List<P2pPeerInfo>.filterWithPublicKey(publicKey: String): List<P2pPeerInfo> =
        filter { it.publicKey == publicKey }

    private fun failWithUnknownRecipient(): Nothing = throw IllegalArgumentException("Recipient unknown")
}