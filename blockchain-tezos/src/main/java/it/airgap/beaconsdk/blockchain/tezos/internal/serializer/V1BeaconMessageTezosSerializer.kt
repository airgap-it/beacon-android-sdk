package it.airgap.beaconsdk.blockchain.tezos.internal.serializer

import it.airgap.beaconsdk.blockchain.tezos.internal.message.v1.V1TezosMessage
import it.airgap.beaconsdk.core.internal.blockchain.serializer.V1BeaconMessageBlockchainSerializer
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.utils.SuperClassSerializer
import kotlinx.serialization.KSerializer

internal class V1BeaconMessageTezosSerializer : V1BeaconMessageBlockchainSerializer {
    override val message: KSerializer<V1BeaconMessage>
        get() = SuperClassSerializer(V1TezosMessage.serializer())
}