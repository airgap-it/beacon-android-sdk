package it.airgap.beaconsdk.internal.transport

import it.airgap.beaconsdk.data.p2p.P2pPairingRequest
import it.airgap.beaconsdk.data.sdk.Origin
import it.airgap.beaconsdk.internal.message.ConnectionMessage
import it.airgap.beaconsdk.internal.storage.ExtendedStorage
import it.airgap.beaconsdk.internal.transport.client.P2pCommunicationClient
import it.airgap.beaconsdk.internal.transport.data.TransportMessage
import it.airgap.beaconsdk.internal.utils.HexString
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.launch
import it.airgap.beaconsdk.internal.utils.suspendTryInternal
import kotlinx.coroutines.flow.*

// @ExperimentalCoroutinesApi
internal class P2pTransport(
    name: String,
    storage: ExtendedStorage,
    private val client: P2pCommunicationClient
) : Transport(name, storage) {
    override val type: Type = Type.P2P

    override val connectionMessages: Flow<InternalResult<ConnectionMessage>> by lazy {
        subscribedPeers
            .onSubscription { emitAll(getKnownPeers()) }
            .flatMapMerge { client.subscribeTo(it) }
            .map { ConnectionMessage.fromInternalResult(it) }
    }

    private val subscribedPeers: MutableSharedFlow<HexString> = MutableSharedFlow(extraBufferCapacity = 1)

    override suspend fun sendMessage(message: String, recipient: String?): InternalResult<Unit> =
        suspendTryInternal {
            val knownPeers = storage.getP2pPeers()
            val recipients = recipient
                ?.let { knownPeers.filterWithPublicKey(it).ifEmpty { failWithUnknownRecipient() } }
                ?: knownPeers

            recipients.launch { client.sendTo(HexString.fromString(it.publicKey), message).getOrThrow() }
        }

    private suspend fun getKnownPeers(): Flow<HexString> =
        storage.getP2pPeers().mapNotNull { HexString.fromStringOrNull(it.publicKey) }.asFlow()

    private fun ConnectionMessage.Companion.fromInternalResult(transportMessage: InternalResult<TransportMessage>): InternalResult<ConnectionMessage> =
        transportMessage.map { ConnectionMessage(Origin.P2P(it.id), it.content) }

    private fun List<P2pPairingRequest>.filterWithPublicKey(publicKey: String): List<P2pPairingRequest> =
        filter { it.publicKey == publicKey }

    private fun failWithUnknownRecipient(): Nothing = throw IllegalArgumentException(ERROR_MESSAGE_UNKNOWN_RECIPIENT)

    companion object {
        const val TAG = "P2pTransport"

        private const val ERROR_MESSAGE_UNKNOWN_RECIPIENT = "Recipient unknown"
    }
}