package it.airgap.blockchain.substrate.internal.serializer

import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.data.Network
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.internal.blockchain.serializer.DataBlockchainSerializer
import it.airgap.beaconsdk.core.internal.utils.SuperClassSerializer
import it.airgap.blockchain.substrate.data.SubstrateError
import it.airgap.blockchain.substrate.data.SubstrateNetwork
import it.airgap.blockchain.substrate.data.SubstratePermission
import kotlinx.serialization.KSerializer

internal class DataSubstrateSerializer : DataBlockchainSerializer {
    override val network: KSerializer<Network>
        get() = SuperClassSerializer(SubstrateNetwork.serializer())

    override val permission: KSerializer<Permission>
        get() = SuperClassSerializer(SubstratePermission.serializer())

    override val error: KSerializer<BeaconError>
        get() = SuperClassSerializer(SubstrateError.serializer())
}