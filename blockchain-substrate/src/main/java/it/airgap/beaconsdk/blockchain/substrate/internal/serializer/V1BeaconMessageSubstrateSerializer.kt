package it.airgap.beaconsdk.blockchain.substrate.internal.serializer

import it.airgap.beaconsdk.core.internal.blockchain.serializer.V1BeaconMessageBlockchainSerializer
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.utils.failWithUnsupportedMessageVersion
import it.airgap.beaconsdk.blockchain.substrate.Substrate
import kotlinx.serialization.KSerializer

internal class V1BeaconMessageSubstrateSerializer : V1BeaconMessageBlockchainSerializer {
    @get:Throws(IllegalArgumentException::class)
    override val message: KSerializer<V1BeaconMessage>
        get() = failWithUnsupportedMessageVersion("1", Substrate.IDENTIFIER)
}