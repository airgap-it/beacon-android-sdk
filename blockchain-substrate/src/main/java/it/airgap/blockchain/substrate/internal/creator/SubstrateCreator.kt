package it.airgap.blockchain.substrate.internal.creator

import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.internal.blockchain.creator.DataBlockchainCreator
import it.airgap.beaconsdk.core.internal.blockchain.creator.V1BeaconMessageBlockchainCreator
import it.airgap.beaconsdk.core.internal.blockchain.creator.V2BeaconMessageBlockchainCreator
import it.airgap.beaconsdk.core.internal.blockchain.creator.V3BeaconMessageBlockchainCreator
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.PermissionBeaconRequest
import it.airgap.beaconsdk.core.message.PermissionBeaconResponse

internal class SubstrateCreator(
    override val data: DataBlockchainCreator,
    override val v1: V1BeaconMessageBlockchainCreator,
    override val v2: V2BeaconMessageBlockchainCreator,
    override val v3: V3BeaconMessageBlockchainCreator,
) : Blockchain.Creator