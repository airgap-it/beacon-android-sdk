package it.airgap.beaconsdk.internal.transport.p2p

import it.airgap.beaconsdk.data.p2p.P2pPeerInfo
import it.airgap.beaconsdk.data.sdk.Origin
import it.airgap.beaconsdk.internal.message.ConnectionMessage
import it.airgap.beaconsdk.internal.storage.ExtendedStorage
import it.airgap.beaconsdk.internal.transport.Transport
import it.airgap.beaconsdk.internal.transport.data.TransportMessage
import it.airgap.beaconsdk.internal.utils.HexString
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.launch
import it.airgap.beaconsdk.internal.utils.tryResult
import kotlinx.coroutines.flow.*

internal class P2pTransport(
    name: String,
    storage: ExtendedStorage,
    private val client: P2pCommunicationClient
) : Transport(name, storage) {
    override val type: Type = Type.P2P

    override val connectionMessages: Flow<InternalResult<ConnectionMessage>> by lazy {
        storage.p2pPeers
            .onEach { if (!it.isPaired) pairP2pPeer(it) }
            .mapNotNull { HexString.fromStringOrNull(it.publicKey) }
            .distinctUntilChanged()
            .filterNot { subscribedPeers.contains(it) }
            .flatMapMerge { subscribeToP2pPeer(it) }
            .map { ConnectionMessage.fromInternalResult(it) }
    }

    private val subscribedPeers: MutableList<HexString> = mutableListOf()

    override suspend fun sendMessage(message: String, recipient: String?): InternalResult<Unit> =
       tryResult {
            val knownPeers = storage.getP2pPeers()
            val recipients = recipient
                ?.let { knownPeers.filterWithPublicKey(it).ifEmpty { failWithUnknownRecipient() } }
                ?: knownPeers

            recipients.launch {
                client.sendTo(HexString.fromString(it.publicKey), message).getOrThrow()
            }
        }

    private suspend fun pairP2pPeer(peerInfo: P2pPeerInfo) {
        val result = client.sendPairingRequest(
            HexString.fromString(peerInfo.publicKey),
            peerInfo.relayServer
        )

        if (result.isSuccess) {
            storage.addP2pPeers(peerInfo.copy(isPaired = true), overwrite = true) { a, b ->
                a.name == b.name
                        && a.publicKey == b.publicKey
                        && a.relayServer == b.relayServer
                        && a.icon == b.icon
                        && a.appUrl == b.appUrl
            }
        }
    }

    private fun subscribeToP2pPeer(publicKey: HexString): Flow<InternalResult<TransportMessage>> {
        subscribedPeers.add(publicKey)

        return client.subscribeTo(publicKey)
    }

    private fun ConnectionMessage.Companion.fromInternalResult(transportMessage: InternalResult<TransportMessage>): InternalResult<ConnectionMessage> =
        transportMessage.map { ConnectionMessage(Origin.P2P(it.id), it.content) }

    private fun List<P2pPeerInfo>.filterWithPublicKey(publicKey: String): List<P2pPeerInfo> =
        filter { it.publicKey == publicKey }

    private fun failWithUnknownRecipient(): Nothing = throw IllegalArgumentException(
        ERROR_MESSAGE_UNKNOWN_RECIPIENT
    )

    companion object {
        const val TAG = "P2pTransport"

        private const val ERROR_MESSAGE_UNKNOWN_RECIPIENT = "Recipient unknown"
    }
}