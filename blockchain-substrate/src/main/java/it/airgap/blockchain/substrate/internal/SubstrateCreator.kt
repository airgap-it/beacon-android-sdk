package it.airgap.blockchain.substrate.internal

import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.PermissionBeaconRequest
import it.airgap.beaconsdk.core.message.PermissionBeaconResponse

internal class SubstrateCreator : Blockchain.Creator {
    override suspend fun extractPermission(
        request: PermissionBeaconRequest,
        response: PermissionBeaconResponse,
    ): Result<Permission> {
        TODO("Not yet implemented")
    }

    override fun v1From(senderId: String, content: BeaconMessage): Result<V1BeaconMessage> {
        TODO("Not yet implemented")
    }

    override fun v2From(senderId: String, content: BeaconMessage): Result<V2BeaconMessage> {
        TODO("Not yet implemented")
    }
}