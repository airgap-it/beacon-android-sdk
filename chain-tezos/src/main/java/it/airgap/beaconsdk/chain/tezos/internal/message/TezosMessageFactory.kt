package it.airgap.beaconsdk.chain.tezos.internal.message

import it.airgap.beaconsdk.chain.tezos.internal.message.v1.V1TezosMessage
import it.airgap.beaconsdk.chain.tezos.internal.message.v2.V2TezosMessage
import it.airgap.beaconsdk.core.internal.chain.Chain
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconMessage

public class TezosMessageFactory internal constructor() : Chain.MessageFactory {
    override val v1: VersionedBeaconMessage.Factory<BeaconMessage, V1BeaconMessage>
        get() = V1TezosMessage.Companion

    override val v2: VersionedBeaconMessage.Factory<BeaconMessage, V2BeaconMessage>
        get() = V2TezosMessage.Companion
}
