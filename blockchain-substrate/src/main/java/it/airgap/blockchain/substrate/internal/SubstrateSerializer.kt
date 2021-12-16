package it.airgap.blockchain.substrate.internal

import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.data.Network
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.utils.SuperClassSerializer
import it.airgap.beaconsdk.core.internal.utils.failWithUnsupportedMessageVersion
import it.airgap.blockchain.substrate.Substrate
import it.airgap.blockchain.substrate.data.SubstrateError
import it.airgap.blockchain.substrate.data.SubstrateNetwork
import it.airgap.blockchain.substrate.data.SubstratePermission
import kotlinx.serialization.KSerializer

internal class SubstrateSerializer : Blockchain.Serializer {

    // -- data --

    override val network: KSerializer<Network>
        get() = SuperClassSerializer(SubstrateNetwork.serializer())

    override val permission: KSerializer<Permission>
        get() = SuperClassSerializer(SubstratePermission.serializer())

    override val error: KSerializer<BeaconError>
        get() = SuperClassSerializer(SubstrateError.serializer())

    // -- VersionedBeaconMessage --

    @get:Throws(IllegalArgumentException::class)
    override val v1: KSerializer<V1BeaconMessage>
        get() = failWithUnsupportedMessageVersion("1", Substrate.IDENTIFIER)

    @get:Throws(IllegalArgumentException::class)
    override val v2: KSerializer<V2BeaconMessage>
        get() = failWithUnsupportedMessageVersion("2", Substrate.IDENTIFIER)
}