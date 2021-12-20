package it.airgap.blockchain.substrate.internal.creator

import it.airgap.beaconsdk.core.internal.blockchain.creator.V2BeaconMessageBlockchainCreator
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconMessage

internal class V2BeaconMessageSubstrateCreator : V2BeaconMessageBlockchainCreator {
    override fun from(senderId: String, message: BeaconMessage): Result<V2BeaconMessage> {
        TODO("Not yet implemented")
    }
}