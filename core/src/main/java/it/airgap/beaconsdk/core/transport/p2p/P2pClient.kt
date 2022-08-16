package it.airgap.beaconsdk.core.transport.p2p

import it.airgap.beaconsdk.core.data.P2pPeer
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.transport.p2p.data.P2pMessage
import it.airgap.beaconsdk.core.transport.data.P2pPairingRequest
import it.airgap.beaconsdk.core.transport.data.P2pPairingResponse
import kotlinx.coroutines.flow.Flow

/**
 * Base for different P2P implementations provided in Beacon.
 */
public interface P2pClient {

    /**
     * Checks if [peer] has been already subscribed.
     */
    public fun isSubscribed(peer: P2pPeer): Boolean

    /**
     * Subscribes to a [peer] returning a [Flow] of received [P2pMessage]
     * instances identified as sent by the subscribed peer or occurred errors represented as a [Result].
     */
    public fun subscribeTo(peer: P2pPeer): Flow<Result<P2pMessage>>?

    /**
     * Removes an active subscription for the [peer].
     */
    public suspend fun unsubscribeFrom(peer: P2pPeer)

    /**
     * Sends a text message to the specified [peer].
     */
    public suspend fun sendTo(peer: P2pPeer, message: String): Result<Unit>

    /**
     * Creates a pairing request.
     */
    public suspend fun createPairingRequest(): Result<P2pPairingRequest>

    /**
     * Creates a pairing response from the [request].
     */
    public suspend fun createPairingResponse(request: P2pPairingRequest): Result<P2pPairingResponse>

    public val pairingResponses: Flow<Result<P2pPairingResponse>>

    /**
     * Sends a pairing response to the specified [peer].
     */
    public suspend fun sendPairingResponse(peer: P2pPeer): Result<Unit>

    /**
     * Creator for [P2pClient] instances.
     *
     * Used to create dynamically registered clients.
     */
    public interface Factory<T : P2pClient> {
        public fun create(dependencyRegistry: DependencyRegistry): T
    }
}