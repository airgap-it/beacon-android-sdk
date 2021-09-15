package it.airgap.beaconsdk.chain.tezos.internal

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.chain.tezos.data.TezosError
import it.airgap.beaconsdk.chain.tezos.internal.message.v1.V1TezosMessage
import it.airgap.beaconsdk.chain.tezos.internal.message.v2.V2TezosMessage
import it.airgap.beaconsdk.chain.tezos.message.TezosRequest
import it.airgap.beaconsdk.chain.tezos.message.TezosResponse
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.internal.chain.Chain
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.utils.PolymorphicSerializer
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.ChainBeaconRequest
import it.airgap.beaconsdk.core.message.ChainBeaconResponse
import kotlinx.serialization.KSerializer

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TezosSerializer internal constructor() : Chain.Serializer {

    // -- BeaconMessage --

    override val requestPayload: KSerializer<ChainBeaconRequest.Payload>
        get() = PolymorphicSerializer(TezosRequest.serializer())

    override val responsePayload: KSerializer<ChainBeaconResponse.Payload>
        get() = PolymorphicSerializer(TezosResponse.serializer())

    // -- data --

    override val error: KSerializer<BeaconError>
        get() = PolymorphicSerializer(TezosError.serializer())

    // -- VersionedBeaconMessage --

    override val v1: VersionedBeaconMessage.Factory<BeaconMessage, V1BeaconMessage>
        get() = V1TezosMessage.Companion

    override val v2: VersionedBeaconMessage.Factory<BeaconMessage, V2BeaconMessage>
        get() = V2TezosMessage.Companion
}
