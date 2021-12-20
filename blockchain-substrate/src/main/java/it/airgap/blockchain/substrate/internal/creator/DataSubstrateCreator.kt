package it.airgap.blockchain.substrate.internal.creator

import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.internal.blockchain.creator.DataBlockchainCreator
import it.airgap.beaconsdk.core.message.PermissionBeaconRequest
import it.airgap.beaconsdk.core.message.PermissionBeaconResponse

internal class DataSubstrateCreator : DataBlockchainCreator {
    override suspend fun extractPermission(
        request: PermissionBeaconRequest,
        response: PermissionBeaconResponse,
    ): Result<Permission> {
        TODO("Not yet implemented")
    }
}