package it.airgap.beaconsdk.blockchain.tezos.internal.creator

import it.airgap.beaconsdk.blockchain.tezos.internal.message.v1.V1TezosMessage
import it.airgap.beaconsdk.core.internal.blockchain.creator.V1BeaconMessageBlockchainCreator
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconMessage

internal class V1BeaconMessageTezosCreator : V1BeaconMessageBlockchainCreator {
    override fun from(senderId: String, message: BeaconMessage): Result<V1BeaconMessage> = runCatching { V1TezosMessage.from(senderId, message) }
}