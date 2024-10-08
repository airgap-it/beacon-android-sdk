package it.airgap.beaconsdk.core.client

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.Account
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.exception.BeaconException
import it.airgap.beaconsdk.core.message.BeaconRequest
import it.airgap.beaconsdk.core.transport.data.PairingRequest

public interface BeaconProducer {
    public val senderId: String

    /**
     * Sends the [request] to the previously paired peer.
     *
     * @throws [BeaconException] if processing and sending the [request] failed.
     */
    @Throws(BeaconException::class)
    public suspend fun request(request: BeaconRequest)

    /**
     * Prepares and triggers peer pairing via transport specified with the [connectionType]. Returns
     * the pairing request, which, depending on the transport used, may require additional handling.
     *
     * @throws [BeaconException] if the process failed.
     */
    @Throws(BeaconException::class)
    public suspend fun pair(connectionType: Connection.Type = Connection.Type.P2P): PairingRequest

    public suspend fun prepareRequest(connectionType: Connection.Type): RequestMetadata

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public data class RequestMetadata(
        public val id: String,
        public val version: String,
        public val senderId: String,
        public val origin: Connection.Id,
        public val destination: Connection.Id?,
        public val account: Account?,
    )
}