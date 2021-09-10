package it.airgap.beaconsdk.core.internal.transport.p2p

import it.airgap.beaconsdk.core.data.beacon.P2pPeer
import it.airgap.beaconsdk.core.internal.transport.p2p.data.P2pMessage
import kotlinx.coroutines.flow.Flow

public class P2pMatrixClient : P2pClient {

    override fun isSubscribed(publicKey: ByteArray): Boolean = throw NotImplementedError()

    override fun subscribeTo(publicKey: ByteArray): Flow<Result<P2pMessage>> {
        throw NotImplementedError()
    }

    override suspend fun unsubscribeFrom(publicKey: ByteArray) {
        throw NotImplementedError()
    }

    override suspend fun sendTo(peer: P2pPeer, message: String): Result<Unit> =
        throw NotImplementedError()

    override suspend fun sendPairingResponse(peer: P2pPeer): Result<Unit> =
        throw NotImplementedError()

    companion object {
        const val TAG = "P2pMatrixClient"
    }
}