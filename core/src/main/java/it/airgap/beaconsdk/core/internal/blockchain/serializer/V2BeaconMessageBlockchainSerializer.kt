package it.airgap.beaconsdk.core.internal.blockchain.serializer

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import kotlinx.serialization.KSerializer

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface V2BeaconMessageBlockchainSerializer {
    public val message: KSerializer<V2BeaconMessage>
}