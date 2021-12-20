package it.airgap.beaconsdk.blockchain.tezos.internal.creator

import it.airgap.beaconsdk.core.internal.blockchain.creator.V3BeaconMessageBlockchainCreator
import it.airgap.beaconsdk.core.internal.message.v3.V3BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconMessage

internal class V3BeaconMessageTezosCreator : V3BeaconMessageBlockchainCreator {
    override fun contentFrom(senderId: String, content: BeaconMessage): Result<V3BeaconMessage.Content> {
        TODO("Not yet implemented")
    }
}