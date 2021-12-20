package it.airgap.beaconsdk.core.internal.blockchain.creator

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.message.v3.V3BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconMessage

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface V3BeaconMessageBlockchainCreator {
    public fun contentFrom(senderId: String, message: BeaconMessage): Result<V3BeaconMessage.Content>
}