package it.airgap.beaconsdk.blockchain.tezos.internal.serializer

import it.airgap.beaconsdk.blockchain.tezos.internal.message.v3.BlockchainV3TezosRequest
import it.airgap.beaconsdk.blockchain.tezos.internal.message.v3.BlockchainV3TezosResponse
import it.airgap.beaconsdk.blockchain.tezos.internal.message.v3.PermissionV3TezosRequest
import it.airgap.beaconsdk.blockchain.tezos.internal.message.v3.PermissionV3TezosResponse
import it.airgap.beaconsdk.core.internal.blockchain.serializer.V3BeaconMessageBlockchainSerializer
import it.airgap.beaconsdk.core.internal.message.v3.BlockchainV3BeaconRequestContent
import it.airgap.beaconsdk.core.internal.message.v3.BlockchainV3BeaconResponseContent
import it.airgap.beaconsdk.core.internal.message.v3.PermissionV3BeaconRequestContent
import it.airgap.beaconsdk.core.internal.message.v3.PermissionV3BeaconResponseContent
import it.airgap.beaconsdk.core.internal.utils.SuperClassSerializer
import kotlinx.serialization.KSerializer

internal class V3BeaconMessageTezosSerializer : V3BeaconMessageBlockchainSerializer {
    override val permissionRequestData: KSerializer<PermissionV3BeaconRequestContent.BlockchainData>
        get() = SuperClassSerializer(PermissionV3TezosRequest.serializer())

    override val blockchainRequestData: KSerializer<BlockchainV3BeaconRequestContent.BlockchainData>
        get() = SuperClassSerializer(BlockchainV3TezosRequest.serializer())

    override val permissionResponseData: KSerializer<PermissionV3BeaconResponseContent.BlockchainData>
        get() = SuperClassSerializer(PermissionV3TezosResponse.serializer())

    override val blockchainResponseData: KSerializer<BlockchainV3BeaconResponseContent.BlockchainData>
        get() = SuperClassSerializer(BlockchainV3TezosResponse.serializer())
}