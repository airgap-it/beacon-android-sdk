package it.airgap.blockchain.substrate.internal.serializer

import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.data.Network
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.internal.blockchain.serializer.DataBlockchainSerializer
import it.airgap.beaconsdk.core.internal.blockchain.serializer.V1BeaconMessageBlockchainSerializer
import it.airgap.beaconsdk.core.internal.blockchain.serializer.V2BeaconMessageBlockchainSerializer
import it.airgap.beaconsdk.core.internal.blockchain.serializer.V3BeaconMessageBlockchainSerializer
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.utils.SuperClassSerializer
import it.airgap.beaconsdk.core.internal.utils.failWithUnsupportedMessageVersion
import it.airgap.blockchain.substrate.Substrate
import it.airgap.blockchain.substrate.data.SubstrateError
import it.airgap.blockchain.substrate.data.SubstrateNetwork
import it.airgap.blockchain.substrate.data.SubstratePermission
import kotlinx.serialization.KSerializer

internal class SubstrateSerializer(
    override val data: DataBlockchainSerializer,
    override val v1: V1BeaconMessageBlockchainSerializer,
    override val v2: V2BeaconMessageBlockchainSerializer,
    override val v3: V3BeaconMessageBlockchainSerializer,
) : Blockchain.Serializer