package it.airgap.beaconsdk.core.internal.blockchain.creator

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconMessage

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface V1BeaconMessageBlockchainCreator {
    public fun from(senderId: String, message: BeaconMessage): Result<V1BeaconMessage>
}