package it.airgap.beaconsdk.core.internal.transport.p2p

import it.airgap.beaconsdk.core.data.beacon.P2pPeer
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.transport.p2p.data.P2pMessage
import kotlinx.coroutines.flow.Flow

public interface P2pClient {
    public fun isSubscribed(publicKey: ByteArray): Boolean
    public fun subscribeTo(publicKey: ByteArray): Flow<Result<P2pMessage>>
    public suspend fun unsubscribeFrom(publicKey: ByteArray)

    public suspend fun sendTo(peer: P2pPeer, message: String): Result<Unit>
    public suspend fun sendPairingResponse(peer: P2pPeer): Result<Unit>

    public interface Factory<T : P2pClient> {
        public fun create(dependencyRegistry: DependencyRegistry): T
    }
}