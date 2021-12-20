package it.airgap.beaconsdk.blockchain.tezos.internal.creator

import it.airgap.beaconsdk.blockchain.tezos.internal.message.v2.V2TezosMessage
import it.airgap.beaconsdk.core.internal.blockchain.creator.V2BeaconMessageBlockchainCreator
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconMessage

internal class V2BeaconMessageTezosCreator : V2BeaconMessageBlockchainCreator {
    override fun from(senderId: String, message: BeaconMessage): Result<V2BeaconMessage> = runCatching { V2TezosMessage.from(senderId, message) }
}