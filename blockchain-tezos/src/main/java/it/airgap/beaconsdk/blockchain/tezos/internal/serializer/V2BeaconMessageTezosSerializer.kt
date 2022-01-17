package it.airgap.beaconsdk.blockchain.tezos.internal.serializer

import it.airgap.beaconsdk.blockchain.tezos.internal.message.v2.V2TezosMessage
import it.airgap.beaconsdk.core.internal.blockchain.serializer.V2BeaconMessageBlockchainSerializer
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.utils.SuperClassSerializer
import kotlinx.serialization.KSerializer

internal class V2BeaconMessageTezosSerializer : V2BeaconMessageBlockchainSerializer {
    override val message: KSerializer<V2BeaconMessage>
        get() = SuperClassSerializer(V2TezosMessage.serializer())
}