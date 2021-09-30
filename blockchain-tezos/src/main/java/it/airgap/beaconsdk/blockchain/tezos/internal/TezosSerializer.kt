package it.airgap.beaconsdk.blockchain.tezos.internal

import it.airgap.beaconsdk.blockchain.tezos.data.TezosError
import it.airgap.beaconsdk.blockchain.tezos.data.TezosNetwork
import it.airgap.beaconsdk.blockchain.tezos.data.TezosPermission
import it.airgap.beaconsdk.blockchain.tezos.internal.message.v1.V1TezosMessage
import it.airgap.beaconsdk.blockchain.tezos.internal.message.v2.V2TezosMessage
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.data.Network
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.utils.SuperClassSerializer
import kotlinx.serialization.KSerializer

internal class TezosSerializer internal constructor() : Blockchain.Serializer {

    // -- data --

    override val network: KSerializer<Network>
        get() = SuperClassSerializer(TezosNetwork.serializer())

    override val permission: KSerializer<Permission>
        get() = SuperClassSerializer(TezosPermission.serializer())

    override val error: KSerializer<BeaconError>
        get() = SuperClassSerializer(TezosError.serializer())

    // -- VersionedBeaconMessage --

    override val v1: KSerializer<V1BeaconMessage>
        get() = SuperClassSerializer(V1TezosMessage.serializer())

    override val v2: KSerializer<V2BeaconMessage>
        get() = SuperClassSerializer(V2TezosMessage.serializer())
}
