package it.airgap.beaconsdk.blockchain.substrate.internal.creator

import it.airgap.beaconsdk.core.internal.blockchain.creator.V3BeaconMessageBlockchainCreator
import it.airgap.beaconsdk.core.internal.message.v3.*
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalState
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.blockchain.substrate.internal.message.v3.BlockchainV3SubstrateRequest
import it.airgap.beaconsdk.blockchain.substrate.internal.message.v3.BlockchainV3SubstrateResponse
import it.airgap.beaconsdk.blockchain.substrate.internal.message.v3.PermissionV3SubstrateRequest
import it.airgap.beaconsdk.blockchain.substrate.internal.message.v3.PermissionV3SubstrateResponse
import it.airgap.beaconsdk.blockchain.substrate.internal.utils.failWithUnknownMessage
import it.airgap.beaconsdk.blockchain.substrate.message.request.BlockchainSubstrateRequest
import it.airgap.beaconsdk.blockchain.substrate.message.request.PermissionSubstrateRequest
import it.airgap.beaconsdk.blockchain.substrate.message.response.BlockchainSubstrateResponse
import it.airgap.beaconsdk.blockchain.substrate.message.response.PermissionSubstrateResponse

internal class V3BeaconMessageSubstrateCreator : V3BeaconMessageBlockchainCreator {
    override fun contentFrom(message: BeaconMessage): Result<V3BeaconMessage.Content> =
        runCatching {
            with(message) {
                when (this) {
                    is PermissionSubstrateRequest -> PermissionV3BeaconRequestContent(
                        blockchainIdentifier,
                        PermissionV3SubstrateRequest.from(this),
                    )
                    is BlockchainSubstrateRequest -> BlockchainV3BeaconRequestContent(
                        blockchainIdentifier,
                        accountId ?: failWithMissingAccountId(),
                        BlockchainV3SubstrateRequest.from(this),
                    )
                    is PermissionSubstrateResponse -> PermissionV3BeaconResponseContent(
                        blockchainIdentifier,
                        PermissionV3SubstrateResponse.from(this),
                    )
                    is BlockchainSubstrateResponse -> BlockchainV3BeaconResponseContent(
                        blockchainIdentifier,
                        BlockchainV3SubstrateResponse.from(this),
                    )
                    else -> failWithUnknownMessage(message)
                }
            }
        }

    private fun failWithMissingAccountId(): Nothing = failWithIllegalState("Value `accountId` is missing in Substrate v3 request.")
}