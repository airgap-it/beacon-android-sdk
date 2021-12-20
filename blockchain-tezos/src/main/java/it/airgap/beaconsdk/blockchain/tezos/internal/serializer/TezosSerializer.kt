package it.airgap.beaconsdk.blockchain.tezos.internal.serializer

import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.internal.blockchain.serializer.DataBlockchainSerializer
import it.airgap.beaconsdk.core.internal.blockchain.serializer.V1BeaconMessageBlockchainSerializer
import it.airgap.beaconsdk.core.internal.blockchain.serializer.V2BeaconMessageBlockchainSerializer
import it.airgap.beaconsdk.core.internal.blockchain.serializer.V3BeaconMessageBlockchainSerializer

internal class TezosSerializer internal constructor(
    override val data: DataBlockchainSerializer,
    override val v1: V1BeaconMessageBlockchainSerializer,
    override val v2: V2BeaconMessageBlockchainSerializer,
    override val v3: V3BeaconMessageBlockchainSerializer,
) : Blockchain.Serializer
