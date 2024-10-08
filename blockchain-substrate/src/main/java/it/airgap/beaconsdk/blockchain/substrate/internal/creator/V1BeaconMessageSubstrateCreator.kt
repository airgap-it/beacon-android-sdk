package it.airgap.beaconsdk.blockchain.substrate.internal.creator

import it.airgap.beaconsdk.core.internal.blockchain.creator.V1BeaconMessageBlockchainCreator
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.utils.failWithUnsupportedMessageVersion
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.blockchain.substrate.Substrate

internal class V1BeaconMessageSubstrateCreator : V1BeaconMessageBlockchainCreator {
    @Throws(IllegalStateException::class)
    override fun from(senderId: String, message: BeaconMessage): Result<V1BeaconMessage> =
        runCatching { failWithUnsupportedMessageVersion("1", Substrate.IDENTIFIER) }
}