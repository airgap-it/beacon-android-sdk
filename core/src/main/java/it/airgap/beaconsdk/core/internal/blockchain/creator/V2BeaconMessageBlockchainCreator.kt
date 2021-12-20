package it.airgap.beaconsdk.core.internal.blockchain.creator

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconMessage

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface V2BeaconMessageBlockchainCreator {
    public fun from(senderId: String, message: BeaconMessage): Result<V2BeaconMessage>
}