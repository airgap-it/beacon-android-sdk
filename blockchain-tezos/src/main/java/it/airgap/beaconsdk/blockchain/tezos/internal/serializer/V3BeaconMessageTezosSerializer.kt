package it.airgap.beaconsdk.blockchain.tezos.internal.serializer

import it.airgap.beaconsdk.core.internal.blockchain.serializer.V3BeaconMessageBlockchainSerializer
import it.airgap.beaconsdk.core.internal.message.v3.BlockchainV3BeaconRequestContent
import it.airgap.beaconsdk.core.internal.message.v3.BlockchainV3BeaconResponseContent
import it.airgap.beaconsdk.core.internal.message.v3.PermissionV3BeaconRequestContent
import it.airgap.beaconsdk.core.internal.message.v3.PermissionV3BeaconResponseContent
import kotlinx.serialization.KSerializer

internal class V3BeaconMessageTezosSerializer : V3BeaconMessageBlockchainSerializer {
    override val permissionRequestData: KSerializer<PermissionV3BeaconRequestContent.ChainData>
        get() = TODO("Not yet implemented")

    override val blockchainRequestData: KSerializer<BlockchainV3BeaconRequestContent.ChainData>
        get() = TODO("Not yet implemented")

    override val permissionResponseData: KSerializer<PermissionV3BeaconResponseContent.ChainData>
        get() = TODO("Not yet implemented")

    override val blockchainResponseData: KSerializer<BlockchainV3BeaconResponseContent.ChainData>
        get() = TODO("Not yet implemented")
}