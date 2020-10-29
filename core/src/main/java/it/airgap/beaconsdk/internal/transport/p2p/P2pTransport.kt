package it.airgap.beaconsdk.internal.transport.p2p

import it.airgap.beaconsdk.data.beacon.Origin
import it.airgap.beaconsdk.data.beacon.P2pPeerInfo
import it.airgap.beaconsdk.internal.message.SerializedBeaconMessage
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

    override val connectionMessages: Flow<InternalResult<SerializedBeaconMessage>> by lazy {
        storage.p2pPeers
            .onEach { if (!it.isPaired) pairP2pPeer(it) }
            .mapNotNull { HexString.fromStringOrNull(it.publicKey) }
            .distinctUntilChanged()
            .filterNot { subscribedPeers.contains(it) }
            .flatMapMerge { subscribeToP2pPeer(it) }
            .map { SerializedBeaconMessage.fromInternalResult(it) }
    }

    private val subscribedPeers: MutableList<HexString> = mutableListOf()

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
            storage.addP2pPeers(peerInfo.copy(isPaired = true), overwrite = true) { a, b ->
                a.name == b.name
                        && a.publicKey == b.publicKey
                        && a.relayServer == b.relayServer
                        && a.icon == b.icon
                        && a.appUrl == b.appUrl
            }
        }
    }

    private fun subscribeToP2pPeer(publicKey: HexString): Flow<InternalResult<P2pMessage>> {
        subscribedPeers.add(publicKey)

        return client.subscribeTo(publicKey)
    }

    private fun SerializedBeaconMessage.Companion.fromInternalResult(
        p2pMessage: InternalResult<P2pMessage>,
    ): InternalResult<SerializedBeaconMessage> = p2pMessage.map { SerializedBeaconMessage(Origin.P2P(it.id), it.content) }

    private fun List<P2pPeerInfo>.filterWithPublicKey(publicKey: String): List<P2pPeerInfo> =
        filter { it.publicKey == publicKey }

    private fun failWithUnknownRecipient(): Nothing = throw IllegalArgumentException("Recipient unknown")
}