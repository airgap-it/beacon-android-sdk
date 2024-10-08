package it.airgap.beaconsdk.core.internal.blockchain.serializer

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import kotlinx.serialization.KSerializer

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface V1BeaconMessageBlockchainSerializer {
    public val message: KSerializer<V1BeaconMessage>
}