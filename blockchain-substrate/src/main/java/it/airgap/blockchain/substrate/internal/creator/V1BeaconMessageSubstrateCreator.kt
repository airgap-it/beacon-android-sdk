package it.airgap.blockchain.substrate.internal.creator

import it.airgap.beaconsdk.core.internal.blockchain.creator.V1BeaconMessageBlockchainCreator
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconMessage

internal class V1BeaconMessageSubstrateCreator : V1BeaconMessageBlockchainCreator {
    override fun from(senderId: String, content: BeaconMessage): Result<V1BeaconMessage> {
        TODO("Not yet implemented")
    }
}