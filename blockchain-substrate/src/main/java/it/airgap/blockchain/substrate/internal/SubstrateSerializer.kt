package it.airgap.blockchain.substrate.internal

import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.data.Network
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import kotlinx.serialization.KSerializer

internal class SubstrateSerializer : Blockchain.Serializer {
    override val network: KSerializer<Network>
        get() = TODO("Not yet implemented")
    override val permission: KSerializer<Permission>
        get() = TODO("Not yet implemented")
    override val error: KSerializer<BeaconError>
        get() = TODO("Not yet implemented")
    override val v1: KSerializer<V1BeaconMessage>
        get() = TODO("Not yet implemented")
    override val v2: KSerializer<V2BeaconMessage>
        get() = TODO("Not yet implemented")
}