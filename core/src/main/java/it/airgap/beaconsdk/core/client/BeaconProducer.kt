package it.airgap.beaconsdk.core.client

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.internal.utils.failWithActiveAccountNotSet
import it.airgap.beaconsdk.core.message.BeaconRequest
import it.airgap.beaconsdk.core.transport.data.PairingMessage
import it.airgap.beaconsdk.core.transport.data.PairingRequest
import kotlinx.coroutines.flow.Flow

public interface BeaconProducer {
    public val senderId: String

    public suspend fun request(request: BeaconRequest)
    public suspend fun pair(connectionType: Connection.Type = Connection.Type.P2P): Result<PairingRequest>

    public fun prepareRequest(connectionType: Connection.Type): RequestMetadata

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public data class RequestMetadata(
        public val id: String,
        public val version: String,
        public val senderId: String,
        public val origin: Origin,
    )
}