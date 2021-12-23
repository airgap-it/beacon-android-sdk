package it.airgap.beaconsdk.blockchain.substrate.internal.serializer

import it.airgap.beaconsdk.core.internal.blockchain.serializer.V2BeaconMessageBlockchainSerializer
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.utils.failWithUnsupportedMessageVersion
import it.airgap.beaconsdk.blockchain.substrate.Substrate
import kotlinx.serialization.KSerializer

internal class V2BeaconMessageSubstrateSerializer : V2BeaconMessageBlockchainSerializer {
    @get:Throws(IllegalArgumentException::class)
    override val message: KSerializer<V2BeaconMessage>
        get() = failWithUnsupportedMessageVersion("2", Substrate.IDENTIFIER)
}