package it.airgap.beaconsdk.blockchain.substrate.internal.serializer

import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateAppMetadata
import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.data.Network
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.internal.blockchain.serializer.DataBlockchainSerializer
import it.airgap.beaconsdk.core.internal.utils.SuperClassSerializer
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateError
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateNetwork
import it.airgap.beaconsdk.blockchain.substrate.data.SubstratePermission
import kotlinx.serialization.KSerializer

internal class DataSubstrateSerializer : DataBlockchainSerializer {
    override val network: KSerializer<Network>
        get() = SuperClassSerializer(SubstrateNetwork.serializer())

    override val permission: KSerializer<Permission>
        get() = SuperClassSerializer(SubstratePermission.serializer())

    override val appMetadata: KSerializer<AppMetadata>
        get() = SuperClassSerializer(SubstrateAppMetadata.serializer())

    override val error: KSerializer<BeaconError>
        get() = SuperClassSerializer(SubstrateError.serializer())
}