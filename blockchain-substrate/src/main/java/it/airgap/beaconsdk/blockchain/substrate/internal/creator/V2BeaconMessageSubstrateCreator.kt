package it.airgap.beaconsdk.blockchain.substrate.internal.creator

import it.airgap.beaconsdk.core.internal.blockchain.creator.V2BeaconMessageBlockchainCreator
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.utils.failWithUnsupportedMessageVersion
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.blockchain.substrate.Substrate

internal class V2BeaconMessageSubstrateCreator : V2BeaconMessageBlockchainCreator {
    @Throws(IllegalStateException::class)
    override fun from(senderId: String, message: BeaconMessage): Result<V2BeaconMessage> =
        runCatching { failWithUnsupportedMessageVersion("2", Substrate.IDENTIFIER) }
}